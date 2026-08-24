package ru.yandex.practicum.analyzer.service;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public interface SnapshotService {

    void handleSnapshot(SensorsSnapshotAvro snapshot);
}
