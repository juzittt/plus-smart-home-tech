package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public List<OrderDto> getAll() {
        List<OrderDto> result = orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Fetched all orders: count={}", result.size());
        return result;
    }

    @Override
    public OrderDto getById(Long id) {
        log.debug("Fetching order: id={}", id);
        return toDto(findOrThrow(id));
    }

    @Override
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
    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        log.info("Creating order: email={}, itemsCount={}",
                request.customerEmail(), request.items().size());

        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderStatus.CREATED);
        order.setStatusDetails(OrderStatus.CREATED.getDescription());
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setQuantity(itemRequest.quantity());
            item.setPrice(itemRequest.price());
            order.addItem(item);

            BigDecimal itemCost = itemRequest.price()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalPrice = totalPrice.add(itemCost);
        }
        order.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, email={}, totalPrice={}",
                saved.getId(), saved.getCustomerEmail(), saved.getTotalPrice());
        return toDto(saved);
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
