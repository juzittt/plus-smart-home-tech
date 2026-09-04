package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> getAll() {
        List<CategoryDto> result = categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Fetched all categories: count={}", result.size());
        return result;
    }

    @Override
    public CategoryDto getById(Long id) {
        log.debug("Fetching category: id={}", id);
        return toDto(findOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        log.info("Creating category: name={}", request.name());

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        Category saved = categoryRepository.save(category);

        log.info("Category created: id={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found: id={}", id);
                    return new NotFoundException("Category not found: id=" + id);
                });
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }
}
