package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<ProductDto> getAllActive() {
        List<ProductDto> result = productRepository.findByActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Fetched active products: count={}", result.size());
        return result;
    }

    @Override
    public ProductDto getById(Long id) {
        log.debug("Fetching product: id={}", id);
        return toDto(findOrThrow(id));
    }

    @Override
    public List<ProductDto> getByCategory(Long categoryId) {
        log.debug("Fetching products by category: categoryId={}", categoryId);
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found: id=" + categoryId);
        }
        return productRepository.findByActiveTrueAndCategoryId(categoryId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<ProductDto> search(String query) {
        log.debug("Searching products: query='{}'", query);
        List<ProductDto> result = productRepository
                .findByActiveTrueAndNameContainingIgnoreCase(query)
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Search completed: query='{}', found={}", query, result.size());
        return result;
    }

    @Override
    @Transactional
    public ProductDto create(CreateProductRequest request) {
        log.info("Creating product: name={}, price={}, categoryId={}",
                request.name(), request.price(), request.categoryId());

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl());
        product.setActive(true);

        if (request.categoryId() != null) {
            product.setCategory(findCategoryOrThrow(request.categoryId()));
        }

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        log.info("Updating product: id={}", id);

        Product product = findOrThrow(id);

        List<String> updatedFields = new ArrayList<>();
        if (request.name() != null) {
            product.setName(request.name());
            updatedFields.add("name");
        }
        if (request.description() != null) {
            product.setDescription(request.description());
            updatedFields.add("description");
        }
        if (request.price() != null) {
            product.setPrice(request.price());
            updatedFields.add("price");
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
            updatedFields.add("imageUrl");
        }
        if (request.active() != null) {
            product.setActive(request.active());
            updatedFields.add("active");
        }
        if (request.categoryId() != null) {
            product.setCategory(findCategoryOrThrow(request.categoryId()));
            updatedFields.add("categoryId");
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}, updatedFields={}", saved.getId(), updatedFields);
        return toDto(saved);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found: id={}", id);
                    return new NotFoundException("Product not found: id=" + id);
                });
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found: id={}", categoryId);
                    return new NotFoundException("Category not found: id=" + categoryId);
                });
    }

    private ProductDto toDto(Product product) {
        CategoryDto categoryDto = product.getCategory() != null
                ? new CategoryDto(
                        product.getCategory().getId(),
                        product.getCategory().getName(),
                        product.getCategory().getDescription())
                : null;

        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                categoryDto,
                product.getImageUrl(),
                product.isActive());
    }
}
