package ru.yandex.practicum.telemetry.collector.handler.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

@Component
public class DeviceRemovedEventHandler extends AbstractHubEventHandler {

    public DeviceRemovedEventHandler(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEventAvro avro = DeviceRemovedEventAvro.newBuilder()
                .setId(event.getDeviceRemoved().getId())
                .build();

        publish(buildHubEvent(event, avro));
    }
}