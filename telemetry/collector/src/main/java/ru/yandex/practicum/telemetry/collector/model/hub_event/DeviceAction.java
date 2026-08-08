package ru.yandex.practicum.telemetry.collector.model.hub_event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceAction {

    @NotBlank
    private String sensorId;

    @NotNull
    private ActionType type;

    private Integer value;
}
