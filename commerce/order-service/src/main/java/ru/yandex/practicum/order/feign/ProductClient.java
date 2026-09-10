package ru.yandex.practicum.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.commerce.api.inventory.ReserveRequest;
import ru.yandex.practicum.commerce.api.inventory.ReserveResponse;
import ru.yandex.practicum.commerce.api.product.ProductDto;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);

    @PostMapping("/api/inventory/release")
    ReserveResponse releaseStock(@RequestBody ReserveRequest request);
}
