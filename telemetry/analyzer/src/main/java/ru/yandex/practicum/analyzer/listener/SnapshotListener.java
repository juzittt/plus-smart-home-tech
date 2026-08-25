package ru.yandex.practicum.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotListener {

    private final SnapshotService snapshotService;

    @KafkaListener(
            topics = "${kafka.topics.snapshots}",
            groupId = "${kafka.consumer.snapshot.group-id}",
            containerFactory = "snapshotKafkaListenerContainerFactory"
    )
    public void handleSnapshot(SensorsSnapshotAvro snapshot, Acknowledgment ack) {
        try {
            log.debug("Received snapshot: hubId={}, sensors_count={}",
                    snapshot.getHubId(), snapshot.getSensorsState().size());

            snapshotService.handleSnapshot(snapshot);

            ack.acknowledge();
            log.debug("Snapshot processed and committed: hubId={}", snapshot.getHubId());

        } catch (Exception e) {
            log.error("Error processing snapshot for hubId={}: {}",
                    snapshot.getHubId(), e.getMessage(), e);
            throw e;
        }
    }
}
