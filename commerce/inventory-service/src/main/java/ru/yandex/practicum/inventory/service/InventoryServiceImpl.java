package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public List<InventoryDto> getAll() {
        List<InventoryDto> result = inventoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
        log.debug("Fetched all inventory records: count={}", result.size());
        return result;
    }

    @Override
    public InventoryDto getByProductId(Long productId) {
        log.debug("Fetching inventory: productId={}", productId);
        return toDto(findByProductIdOrThrow(productId));
    }

    @Override
    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        log.info("Creating inventory record: productId={}, quantity={}",
                request.productId(), request.quantity());

        if (inventoryRepository.existsByProductId(request.productId())) {
            log.warn("Inventory record already exists: productId={}", request.productId());
            throw new InsufficientStockException(
                    "Inventory record already exists: productId=" + request.productId());
        }

        InventoryItem item = new InventoryItem();
        item.setProductId(request.productId());
        item.setQuantity(request.quantity());
        item.setReservedQuantity(0);

        InventoryItem saved = inventoryRepository.save(item);
        log.info("Inventory record created: id={}, productId={}, quantity={}",
                saved.getId(), saved.getProductId(), saved.getQuantity());
        return toDto(saved);
    }

    @Override
    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        log.info("Updating quantity: productId={}, newQuantity={}",
                request.productId(), request.quantity());

        InventoryItem item = findByProductIdOrThrow(request.productId());
        int oldQuantity = item.getQuantity();
        item.setQuantity(request.quantity());

        InventoryItem saved = inventoryRepository.save(item);
        log.info("Quantity updated: productId={}, old={}, new={}",
                request.productId(), oldQuantity, saved.getQuantity());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        log.info("Reserving stock: productId={}, quantity={}",
                request.productId(), request.quantity());

        InventoryItem item = findByProductIdOrThrow(request.productId());
        int available = item.getAvailableQuantity();

        if (available < request.quantity()) {
            log.warn("Insufficient stock: productId={}, available={}, requested={}",
                    request.productId(), available, request.quantity());
            throw new InsufficientStockException(String.format(
                    "Insufficient stock: productId=%d, available=%d, requested=%d",
                    request.productId(), available, request.quantity()));
        }

        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        InventoryItem saved = inventoryRepository.save(item);

        log.info("Stock reserved: productId={}, reserved={}, available={}",
                request.productId(), saved.getReservedQuantity(), saved.getAvailableQuantity());

        return new ReserveResponse(
                true,
                saved.getAvailableQuantity(),
                "Stock reserved successfully");
    }

    private InventoryItem findByProductIdOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Inventory record not found: productId={}", productId);
                    return new NotFoundException(
                            "Inventory record not found: productId=" + productId);
                });
    }

    private InventoryDto toDto(InventoryItem item) {
        return new InventoryDto(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity());
    }
}
