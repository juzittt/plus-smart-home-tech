package ru.yandex.practicum.telemetry.collector.model.sensor_event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LightSensorEvent extends SensorEvent {

    @NotNull
    private Integer linkQuality;

    @NotNull
    private Integer luminosity;

    @Override
    public SensorEventType getType() {
        return SensorEventType.LIGHT_SENSOR_EVENT;
    }
}
