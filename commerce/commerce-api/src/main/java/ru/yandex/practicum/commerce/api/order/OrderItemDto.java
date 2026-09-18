package ru.yandex.practicum.commerce.api.order;

import java.math.BigDecimal;

public record OrderItemDto(

        Long id,

        Long productId,

        String productName,

        Integer quantity,

        BigDecimal price
) {
}