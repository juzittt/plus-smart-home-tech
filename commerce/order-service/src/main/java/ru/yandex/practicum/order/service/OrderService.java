package ru.yandex.practicum.order.service;

import ru.yandex.practicum.commerce.api.order.CreateOrderRequest;
import ru.yandex.practicum.commerce.api.order.OrderDto;

import java.util.List;

public interface OrderService {

    List<OrderDto> getAll();

    OrderDto getById(Long id);

    List<OrderDto> getByEmail(String email);

    OrderDto create(CreateOrderRequest request);
}
