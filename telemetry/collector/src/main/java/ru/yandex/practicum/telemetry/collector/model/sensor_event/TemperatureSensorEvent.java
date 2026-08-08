package ru.yandex.practicum.telemetry.collector.model.sensor_event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemperatureSensorEvent extends SensorEvent {

    @NotNull
    private Integer temperatureC;

    @NotNull
    private Integer temperatureF;

    @Override
    public SensorEventType getType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }
}
