package ru.yandex.practicum.telemetry.collector.service;

import ru.yandex.practicum.telemetry.collector.model.hub_event.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor_event.SensorEvent;

public interface EventService {
    void publishSensorEvent(SensorEvent event);
    void publishHubEvent(HubEvent event);
}
