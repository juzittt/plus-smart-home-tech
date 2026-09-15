package ru.yandex.practicum.order.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.api.inventory.ReserveRequest;
import ru.yandex.practicum.commerce.api.inventory.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        log.warn("InventoryClient fallback triggered: {}", cause.getMessage());

        return new InventoryClient() {
            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                log.error("Inventory service unavailable, productId={}",
                        request.productId(), cause);
                throw new InventoryServiceUnavailableException(
                        "Inventory service unavailable for productId=" + request.productId(), cause);
            }


            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                log.error("Inventory service unavailable during release, productId={}",
                        request.productId(), cause);
                return new ReserveResponse(false, null,
                        "Inventory service unavailable, compensation skipped");
            }
        };
    }
}