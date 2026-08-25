package ru.yandex.practicum.telemetry.collector.handler.hub;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractHubEventHandler implements HubEventHandler {
    private final EventPublisher eventPublisher;

    HubEventAvro buildHubEvent(HubEventProto proto, SpecificRecordBase payload) {
        return HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(protoTimestampToInstant(proto.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private Instant protoTimestampToInstant (Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    void publish(HubEventAvro message) {
        eventPublisher.publishHubEvent(message);
    }
}
