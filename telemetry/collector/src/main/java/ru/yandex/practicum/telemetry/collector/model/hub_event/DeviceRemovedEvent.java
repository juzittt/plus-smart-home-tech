package ru.yandex.practicum.telemetry.collector.model.hub_event;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRemovedEvent extends HubEvent{

    @NotBlank
    private String id;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }
}
