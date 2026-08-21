package ru.yandex.practicum.telemetry.collector.handler.sensor;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSensorEventHandler implements SensorEventHandler{
    protected final EventPublisher eventPublisher;

    protected SensorEventAvro buildSensorEvent(SensorEventProto proto, SpecificRecordBase payload){
        return SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(protoTimestampToInstant(proto.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    Instant protoTimestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    protected void publish(SensorEventAvro message) {
        eventPublisher.publishSensorEvent(message);
    }
}
