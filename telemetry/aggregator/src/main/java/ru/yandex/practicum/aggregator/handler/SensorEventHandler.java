package ru.yandex.practicum.aggregator.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.aggregator.service.SnapshotService;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorEventHandler {

    private final SnapshotService snapshotService;
    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    private String snapshotsTopic;

    @KafkaListener(
            topics = "${kafka.topics.sensors:telemetry.sensors.v1}",
            groupId = "${kafka.consumer.group-id:aggregator}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSensorEvent(SensorEventAvro event) {
        log.debug("Received sensor event: hubId={}, sensorId={}",
                event.getHubId(), event.getId());

        Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotService.updateState(event);

        if (updatedSnapshot.isPresent()) {
            SensorsSnapshotAvro snapshot = updatedSnapshot.get();
            kafkaTemplate.send(snapshotsTopic, snapshot.getHubId(), snapshot);

            log.info("Snapshot sent to Kafka: hubId={}, sensors_count={}",
                    snapshot.getHubId(), snapshot.getSensorsState().size());
        } else {
            log.debug("Snapshot not updated (duplicate or outdated): hubId={}, sensorId={}",
                    event.getHubId(), event.getId());
        }
    }
}