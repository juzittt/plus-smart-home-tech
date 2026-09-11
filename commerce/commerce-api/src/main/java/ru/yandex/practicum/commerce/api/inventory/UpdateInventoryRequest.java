package ru.yandex.practicum.commerce.api.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(

        @NotNull(message = "ID товара обязателен")
        Long productId,

        @NotNull(message = "Количество обязательно")
        @Min(value = 0, message = "Количество не может быть отрицательным")
        Integer quantity
) {
}
