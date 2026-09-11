package ru.yandex.practicum.commerce.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(

        Long id,

        String customerName,

        String customerEmail,

        String status,

        BigDecimal totalPrice,

        String statusDetails,

        LocalDateTime createdAt,

        List<OrderItemDto> items
) {
}
