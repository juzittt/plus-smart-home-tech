package ru.yandex.practicum.telemetry.collector.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.sensor_event.*;

@Mapper(componentModel = "spring")
public interface SensorEventMapper {

    ClimateSensorAvro toClimateAvro(ClimateSensorEvent event);

    LightSensorAvro toLightAvro(LightSensorEvent event);

    MotionSensorAvro toMotionAvro(MotionSensorEvent event);

    SwitchSensorAvro toSwitchAvro(SwitchSensorEvent event);

    TemperatureSensorAvro toTemperatureAvro(TemperatureSensorEvent event);

    default SensorEventAvro toAvro(SensorEvent event) {
        return switch (event) {
            case ClimateSensorEvent e -> buildSensorEvent(e, toClimateAvro(e));
            case LightSensorEvent e -> buildSensorEvent(e, toLightAvro(e));
            case MotionSensorEvent e -> buildSensorEvent(e, toMotionAvro(e));
            case SwitchSensorEvent e -> buildSensorEvent(e, toSwitchAvro(e));
            case TemperatureSensorEvent e -> buildSensorEvent(e, toTemperatureAvro(e));
            default -> throw new IllegalArgumentException(
                    "Unknown sensor event type: " + event.getType());
        };
    }

    private SensorEventAvro buildSensorEvent(SensorEvent event, Object payload) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
