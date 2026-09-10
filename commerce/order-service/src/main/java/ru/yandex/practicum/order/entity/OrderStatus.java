package ru.yandex.practicum.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    CREATED("Заказ создан и ожидает обработки"),
    CONFIRMED("Заказ подтверждён, товары зарезервированы"),
    CANCELLED("Заказ отменён"),
    COMPLETED("Заказ выполнен");

    private final String description;
}