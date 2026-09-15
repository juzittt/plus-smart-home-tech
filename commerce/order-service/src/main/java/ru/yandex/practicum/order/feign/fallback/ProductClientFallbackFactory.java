package ru.yandex.practicum.order.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.ProductClient;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        log.warn("ProductClient fallback triggered: {}", cause.getMessage());

        return id -> {
            log.error("Product service unavailable, productId={}", id, cause);
            throw new ProductServiceUnavailableException(
                    "Product service unavailable for productId=" + id, cause);
        };
    }
}