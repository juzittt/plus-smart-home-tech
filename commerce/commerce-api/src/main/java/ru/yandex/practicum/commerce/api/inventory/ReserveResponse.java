package ru.yandex.practicum.commerce.api.inventory;

public record ReserveResponse(

        boolean success,

        Integer availableQuantity,

        String message
) {
}
