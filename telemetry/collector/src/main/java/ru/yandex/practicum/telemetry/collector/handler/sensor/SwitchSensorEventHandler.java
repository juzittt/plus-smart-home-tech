package ru.yandex.practicum.telemetry.collector.handler.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

@Component
public class SwitchSensorEventHandler extends AbstractSensorEventHandler {

    public SwitchSensorEventHandler(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorProto proto = event.getSwitchSensor();

        SwitchSensorAvro avro = SwitchSensorAvro.newBuilder()
                .setState(proto.getState())
                .build();

        publish(buildSensorEvent(event, avro));
    }
}