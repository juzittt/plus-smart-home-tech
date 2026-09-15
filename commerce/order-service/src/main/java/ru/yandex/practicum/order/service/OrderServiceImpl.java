package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.commerce.api.inventory.ReserveRequest;
import ru.yandex.practicum.commerce.api.order.CreateOrderRequest;
import ru.yandex.practicum.commerce.api.order.OrderDto;
import ru.yandex.practicum.commerce.api.order.OrderItemDto;
import ru.yandex.practicum.commerce.api.order.OrderItemRequest;
import ru.yandex.practicum.commerce.api.product.ProductDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        List<OrderDto> result = orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Fetched all orders: count={}", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        log.debug("Fetching order: id={}", id);
        return toDto(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getByEmail(String email) {
        log.debug("Fetching orders by email: {}", email);
        List<OrderDto> result = orderRepository.findByCustomerEmail(email)
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Found orders for email {}: count={}", email, result.size());
        return result;
    }

    @Override
    public OrderDto create(CreateOrderRequest request) {
        log.info("Creating order: email={}, itemsCount={}",
                request.customerEmail(), request.items().size());

        Map<Long, Integer> quantityByProductId = aggregateQuantities(request.items());

        Map<Long, ServiceCallResult<ProductDto>> productResults =
                fetchProductResults(quantityByProductId.keySet());

        for (ServiceCallResult<ProductDto> result : productResults.values()) {
            if (result instanceof ServiceCallResult.Failure<?>(String reason)) {
                throw new OrderProcessingException(reason);
            }
        }

        boolean catalogDegraded = productResults.values().stream()
                .anyMatch(ServiceCallResult::isDegraded);

        Map<Long, ServiceCallResult<Boolean>> reservationResults = catalogDegraded
                ? emptyReservations(quantityByProductId.keySet())
                : reserveAll(quantityByProductId);

        for (ServiceCallResult<Boolean> result : reservationResults.values()) {
            if (result instanceof ServiceCallResult.Failure<?>(String reason)) {
                compensateSuccessfulReservations(reservationResults, quantityByProductId);
                throw new OrderProcessingException(reason);
            }
        }

        boolean inventoryDegraded = reservationResults.values().stream()
                .anyMatch(ServiceCallResult::isDegraded);

        OrderStatus finalStatus = (catalogDegraded || inventoryDegraded)
                ? OrderStatus.PENDING_CONFIRMATION
                : OrderStatus.CONFIRMED;

        try {
            Order saved = transactionTemplate.execute(status -> {
                Order order = buildOrder(request, productResults, finalStatus);
                return orderRepository.save(order);
            });

            if (saved == null) {
                throw new OrderProcessingException("Failed to save order");
            }

            log.info("Order saved: id={}, status={}, email={}, totalPrice={}",
                    saved.getId(), saved.getStatus(),
                    saved.getCustomerEmail(), saved.getTotalPrice());
            return toDto(saved);

        } catch (Exception e) {
            if (finalStatus == OrderStatus.CONFIRMED) {
                compensateSuccessfulReservations(reservationResults, quantityByProductId);
            }

            if (e instanceof OrderProcessingException ope) {
                throw ope;
            }
            throw new OrderProcessingException(
                    "Failed to create order: " + e.getMessage(), e);
        }
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderItemRequest> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum));
    }

    private Map<Long, ServiceCallResult<ProductDto>> fetchProductResults(Set<Long> productIds) {
        Map<Long, ServiceCallResult<ProductDto>> results = new HashMap<>();

        for (Long productId : productIds) {
            try {
                ProductDto product = productClient.getProductById(productId);

                if (product.active() == null || !product.active()) {
                    log.warn("Product is not active: productId={}", productId);
                    results.put(productId, new ServiceCallResult.Failure<>(
                            "Product is not active: productId=" + productId));
                } else {
                    results.put(productId, new ServiceCallResult.Success<>(product));
                }

            } catch (FeignException.NotFound e) {
                log.warn("Product not found: productId={}", productId);
                results.put(productId, new ServiceCallResult.Failure<>(
                        "Product not found: productId=" + productId));

            } catch (ProductServiceUnavailableException e) {
                log.warn("Product service unavailable: productId={}", productId);
                results.put(productId, new ServiceCallResult.Degraded<>(
                        "Product service unavailable: productId=" + productId));

            } catch (FeignException e) {
                log.error("Failed to fetch product {}: status={}, message={}",
                        productId, e.status(), e.getMessage());
                results.put(productId, new ServiceCallResult.Degraded<>(
                        "Product service error: productId=" + productId));
            }
        }

        return results;
    }

    private Map<Long, ServiceCallResult<Boolean>> emptyReservations(Set<Long> productIds) {
        Map<Long, ServiceCallResult<Boolean>> results = new HashMap<>();
        for (Long productId : productIds) {
            results.put(productId, new ServiceCallResult.Degraded<>(
                    "Skipped due to catalog unavailability"));
        }
        return results;
    }

    private Map<Long, ServiceCallResult<Boolean>> reserveAll(Map<Long, Integer> quantities) {
        Map<Long, ServiceCallResult<Boolean>> results = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            try {
                inventoryClient.reserveStock(new ReserveRequest(productId, qty));
                results.put(productId, new ServiceCallResult.Success<>(true));
                log.info("Reserved: productId={}, quantity={}", productId, qty);

            } catch (FeignException.NotFound e) {
                log.warn("Inventory record not found: productId={}", productId);
                results.put(productId, new ServiceCallResult.Failure<>(
                        "Inventory record not found: productId=" + productId));

            } catch (FeignException.Conflict e) {
                log.warn("Insufficient stock: productId={}, requested={}", productId, qty);
                results.put(productId, new ServiceCallResult.Failure<>(
                        "Insufficient stock: productId=" + productId));

            } catch (InventoryServiceUnavailableException e) {
                log.warn("Inventory service unavailable: productId={}", productId);
                results.put(productId, new ServiceCallResult.Degraded<>(
                        "Inventory service unavailable: productId=" + productId));

            } catch (FeignException e) {
                log.error("Failed to reserve product {}: status={}, message={}",
                        productId, e.status(), e.getMessage());
                results.put(productId, new ServiceCallResult.Degraded<>(
                        "Inventory service error: productId=" + productId));
            }
        }

        return results;
    }

    private void compensateSuccessfulReservations(
            Map<Long, ServiceCallResult<Boolean>> reservationResults,
            Map<Long, Integer> quantities) {

        List<Long> reservedIds = reservationResults.entrySet().stream()
                .filter(e -> e.getValue().isSuccess())
                .map(Map.Entry::getKey)
                .toList();

        if (reservedIds.isEmpty()) {
            return;
        }

        log.warn("Compensating reservations for {} products", reservedIds.size());
        for (Long productId : reservedIds) {
            try {
                inventoryClient.releaseStock(
                        new ReserveRequest(productId, quantities.get(productId)));
                log.info("Released reservation: productId={}", productId);
            } catch (Exception releaseError) {
                log.error("CRITICAL: Failed to release reservation for productId={}: {}",
                        productId, releaseError.getMessage(), releaseError);
            }
        }
    }

    private Order buildOrder(CreateOrderRequest request,
                             Map<Long, ServiceCallResult<ProductDto>> productResults,
                             OrderStatus status) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(status);
        order.setStatusDetails(status == OrderStatus.PENDING_CONFIRMATION
                ? "Order requires manual verification: some data is unavailable"
                : status.getDescription());
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest item : request.items()) {
            ServiceCallResult<ProductDto> result = productResults.get(item.productId());
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.productId());
            orderItem.setQuantity(item.quantity());

            ProductDto product = result.getValueOrNull();
            if (product != null) {
                orderItem.setProductName(product.name());
                orderItem.setPrice(product.price());
                BigDecimal lineTotal = product.price()
                        .multiply(BigDecimal.valueOf(item.quantity()));
                totalPrice = totalPrice.add(lineTotal);
            } else {
                orderItem.setProductName("Product #" + item.productId() + " (pending verification)");
                orderItem.setPrice(BigDecimal.ZERO);
            }
            order.addItem(orderItem);
        }

        order.setTotalPrice(totalPrice);
        return order;
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.warn("Order not found: id={}", id);
                    return new NotFoundException("Order not found: id=" + id);
                });
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems()
                .stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreatedAt(),
                itemDtos);
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice());
    }
}