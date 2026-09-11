package ru.yandex.practicum.product.service;


import ru.yandex.practicum.commerce.api.product.CreateProductRequest;
import ru.yandex.practicum.commerce.api.product.ProductDto;
import ru.yandex.practicum.commerce.api.product.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    List<ProductDto> getAllActive();

    ProductDto getById(Long id);

    List<ProductDto> getByCategory(Long categoryId);

    List<ProductDto> search(String query);

    ProductDto create(CreateProductRequest request);

    ProductDto update(Long id, UpdateProductRequest request);
}
