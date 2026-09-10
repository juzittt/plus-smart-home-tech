package ru.yandex.practicum.inventory.service;

import ru.yandex.practicum.commerce.api.inventory.InventoryDto;
import ru.yandex.practicum.commerce.api.inventory.ReserveRequest;
import ru.yandex.practicum.commerce.api.inventory.ReserveResponse;
import ru.yandex.practicum.commerce.api.inventory.UpdateInventoryRequest;

import java.util.List;

public interface InventoryService {

    List<InventoryDto> getAll();

    InventoryDto getByProductId(Long productId);

    InventoryDto create(UpdateInventoryRequest request);

    InventoryDto update(UpdateInventoryRequest request);

    ReserveResponse reserve(ReserveRequest request);

    ReserveResponse release(ReserveRequest request);
}
