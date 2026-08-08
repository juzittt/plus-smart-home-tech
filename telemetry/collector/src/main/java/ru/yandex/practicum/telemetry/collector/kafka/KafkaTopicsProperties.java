package ru.yandex.practicum.telemetry.collector.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicsProperties(String sensors, String hubs) {
}
