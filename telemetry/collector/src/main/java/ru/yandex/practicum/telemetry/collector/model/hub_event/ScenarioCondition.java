package ru.yandex.practicum.telemetry.collector.model.hub_event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScenarioCondition {

    @NotNull
    private String sensorId;

    @NotNull
    private ConditionType type;

    @NotNull
    private ConditionOperation operation;
    private Object value;
}
