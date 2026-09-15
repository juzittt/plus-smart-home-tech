package ru.yandex.practicum.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductServiceUnavailableException(Throwable cause) {
        super("Product service is unavailable", cause);
    }
}