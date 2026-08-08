package ru.yandex.practicum.telemetry.collector.publisher;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public interface EventPublisher {
    void publishSensorEvent(SensorEventAvro event);
    void publishHubEvent(HubEventAvro event);
}
