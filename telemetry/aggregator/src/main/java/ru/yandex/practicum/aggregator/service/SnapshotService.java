package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        log.debug("Processing sensor event: hubId={}, sensorId={}, timestamp={}",
                event.getHubId(), event.getId(), event.getTimestamp());

        String hubId = event.getHubId();

        Object lock = locks.computeIfAbsent(hubId, k -> new Object());

        synchronized (lock) {
            SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, k ->
                    SensorsSnapshotAvro.newBuilder()
                            .setHubId(hubId)
                            .setTimestamp(event.getTimestamp())
                            .setSensorsState(new ConcurrentHashMap<>())
                            .build()
            );

            return updateSnapshotIfNewer(snapshot, event);
        }
    }

    private Optional<SensorsSnapshotAvro> updateSnapshotIfNewer(SensorsSnapshotAvro snapshot, SensorEventAvro event) {

        String sensorId = event.getId();
        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(event.getTimestamp()) ||
                    oldState.getTimestamp().equals(event.getTimestamp()) ||
                    Objects.equals(oldState.getData(), event.getPayload())) {

                log.debug("Ignoring event: sensor={}, old_timestamp={}, event_timestamp={}",
                        sensorId, oldState.getTimestamp(), event.getTimestamp());
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        Map<String, SensorStateAvro> sensorsState = new ConcurrentHashMap<>(snapshot.getSensorsState());
        sensorsState.put(sensorId, newState);

        SensorsSnapshotAvro updated = SensorsSnapshotAvro.newBuilder()
                .setHubId(snapshot.getHubId())
                .setTimestamp(event.getTimestamp())
                .setSensorsState(sensorsState)
                .build();

        snapshots.put(snapshot.getHubId(), updated);

        log.info("Snapshot updated: hubId={}, sensorId={}, total_sensors={}",
                snapshot.getHubId(), sensorId, sensorsState.size());

        return Optional.of(updated);
    }
}
