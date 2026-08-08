package ru.yandex.practicum.telemetry.collector.model.sensor_event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MotionSensorEvent extends SensorEvent {

    @NotNull
    private Integer linkQuality;

    @NotNull
    private Boolean motion;

    @NotNull
    private Integer voltage;

    @Override
    public SensorEventType getType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }
}
