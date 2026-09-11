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
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        Map<Long, ProductDto> productsById = fetchProducts(quantityByProductId.keySet());

        validateProductsActive(productsById);

        List<Long> reservedProductIds = new ArrayList<>();
        try {
            reserveAll(quantityByProductId, reservedProductIds);

            Order saved = transactionTemplate.execute(status -> {
                Order order = buildOrder(request, productsById);
                return orderRepository.save(order);
            });

            if (saved == null) {
                throw new OrderProcessingException("Не удалось сохранить заказ");
            }

            log.info("Order confirmed: id={}, email={}, totalPrice={}",
                    saved.getId(), saved.getCustomerEmail(), saved.getTotalPrice());
            return toDto(saved);

        } catch (Exception e) {
            compensateReservations(reservedProductIds, quantityByProductId);

            if (e instanceof OrderProcessingException) {
                throw e;
            }
            throw new OrderProcessingException(
                    "Не удалось создать заказ: " + e.getMessage(), e);
        }
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderItemRequest> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum));
    }

    private Map<Long, ProductDto> fetchProducts(Set<Long> productIds) {
        Map<Long, ProductDto> result = new HashMap<>();
        for (Long productId : productIds) {
            try {
                ProductDto product = productClient.getProductById(productId);
                result.put(productId, product);
            } catch (FeignException.NotFound e) {
                log.warn("Product not found: productId={}", productId);
                throw new OrderProcessingException(
                        "Товар не найден: productId=" + productId);
            } catch (FeignException e) {
                log.error("Failed to fetch product {}: status={}, message={}",
                        productId, e.status(), e.getMessage());
                throw new OrderProcessingException(
                        "Ошибка получения данных товара: productId=" + productId, e);
            }
        }
        return result;
    }

    private void validateProductsActive(Map<Long, ProductDto> products) {
        for (Map.Entry<Long, ProductDto> entry : products.entrySet()) {
            Long id = entry.getKey();
            ProductDto product = entry.getValue();
            if (product.active() == null || !product.active()) {
                log.warn("Product is not active: productId={}", id);
                throw new OrderProcessingException(
                        "Товар снят с продажи: productId=" + id);
            }
        }
    }

    private void reserveAll(Map<Long, Integer> quantities, List<Long> reservedIds) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            try {
                inventoryClient.reserveStock(new ReserveRequest(productId, qty));
                reservedIds.add(productId);
                log.info("Reserved: productId={}, quantity={}", productId, qty);
            } catch (FeignException.NotFound e) {
                log.warn("Inventory record not found: productId={}", productId);
                throw new OrderProcessingException(
                        "Складская запись не найдена: productId=" + productId);
            } catch (FeignException.Conflict e) {
                log.warn("Insufficient stock: productId={}, requested={}", productId, qty);
                throw new OrderProcessingException(
                        "Недостаточно товара: productId=" + productId);
            } catch (FeignException e) {
                log.error("Failed to reserve product {}: status={}, message={}",
                        productId, e.status(), e.getMessage());
                throw new OrderProcessingException(
                        "Ошибка резервирования товара: productId=" + productId, e);
            }
        }
    }

    private void compensateReservations(List<Long> reservedIds,
                                        Map<Long, Integer> quantities) {
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
                             Map<Long, ProductDto> products) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setStatusDetails(OrderStatus.CONFIRMED.getDescription());
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest item : request.items()) {
            ProductDto product = products.get(item.productId());
            BigDecimal price = product.price();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.quantity()));
            totalPrice = totalPrice.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.productId());
            orderItem.setProductName(product.name());
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(price);
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