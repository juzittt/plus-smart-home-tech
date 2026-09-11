package ru.yandex.practicum.commerce.api.inventory;

public record InventoryDto(

        Long id,

        Long productId,

        Integer quantity,

        Integer reservedQuantity,

        Integer availableQuantity
) {
}
