package ru.yandex.practicum.telemetry.collector.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaTopicsProperties;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    @Override
    public void publishSensorEvent(SensorEventAvro event) {
        send(topics.sensors(), event.getHubId(), event.getTimestamp().toEpochMilli(), event);
    }

    @Override
    public void publishHubEvent(HubEventAvro event) {
        send(topics.hubs(), event.getHubId(), event.getTimestamp().toEpochMilli(), event);
    }

    private void send(String topic, String key, long timestamp, SpecificRecordBase value) {
        CompletableFuture<SendResult<String, SpecificRecordBase>> future =
                kafkaTemplate.send(topic, null, timestamp, key, value);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send message to topic '{}': {}", topic, ex.getMessage(), ex);
            } else {
                log.debug("Message sent to topic '{}', partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
