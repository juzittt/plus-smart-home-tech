package ru.yandex.practicum.commerce.api.product;

import java.math.BigDecimal;

public record ProductDto(

        Long id,

        String name,

        String description,

        BigDecimal price,

        CategoryDto category,

        String imageUrl,

        Boolean active
) {
}