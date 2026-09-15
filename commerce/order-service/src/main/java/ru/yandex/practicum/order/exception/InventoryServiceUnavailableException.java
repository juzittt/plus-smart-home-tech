package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

    public InventoryServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public InventoryServiceUnavailableException(Throwable cause) {
        super("Inventory service is unavailable", cause);
    }
}