package ru.yandex.practicum.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    CREATED("Заказ создан и ожидает обработки");

    private final String description;
}
