package ru.yandex.practicum.product.service;


import ru.yandex.practicum.commerce.api.product.CategoryDto;
import ru.yandex.practicum.commerce.api.product.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAll();

    CategoryDto getById(Long id);

    CategoryDto create(CreateCategoryRequest request);
}
