package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.collector.model.hub_event.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor_event.SensorEvent;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final EventPublisher eventPublisher;


    @Override
    public void publishSensorEvent(SensorEvent event) {
        log.debug("Processing sensor event: type={}, id={}", event.getType(), event.getId());

        SensorEventAvro sensorEventAvro = sensorEventMapper.toAvro(event);
        eventPublisher.publishSensorEvent(sensorEventAvro);

        log.debug("Sensor event published: type={}, id={}", event.getType(), event.getId());
    }

    @Override
    public void publishHubEvent(HubEvent event) {
        log.debug("Processing hub event: type={}, id={}", event.getType(), event.getHubId());

        HubEventAvro hubEventAvro = hubEventMapper.toAvro(event);
        eventPublisher.publishHubEvent(hubEventAvro);

        log.debug("Hub event published: type={}, hubId={}", event.getType(), event.getHubId());
    }
}
