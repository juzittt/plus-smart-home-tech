package ru.yandex.practicum.order.service;

public sealed interface ServiceCallResult<T>
        permits ServiceCallResult.Success,
                ServiceCallResult.Failure,
                ServiceCallResult.Degraded {

    record Success<T>(T value) implements ServiceCallResult<T> {}

    record Failure<T>(String reason) implements ServiceCallResult<T> {}

    record Degraded<T>(String reason) implements ServiceCallResult<T> {}

    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    default boolean isDegraded() {
        return this instanceof Degraded<?>;
    }

    @SuppressWarnings("unchecked")
    default T getValueOrNull() {
        if (this instanceof Success<?> s) {
            return (T) s.value();
        }
        return null;
    }
}