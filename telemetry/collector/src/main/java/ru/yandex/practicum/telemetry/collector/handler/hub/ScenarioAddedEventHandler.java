package ru.yandex.practicum.telemetry.collector.handler.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.publisher.EventPublisher;

import java.util.List;

@Component
public class ScenarioAddedEventHandler extends AbstractHubEventHandler {

    public ScenarioAddedEventHandler(EventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto proto = event.getScenarioAdded();

        ScenarioAddedEventAvro avro = ScenarioAddedEventAvro.newBuilder()
                .setName(proto.getName())
                .setConditions(toConditionAvroList(proto.getConditionList()))
                .setActions(toActionAvroList(proto.getActionList()))
                .build();

        publish(buildHubEvent(event, avro));
    }

    private List<ScenarioConditionAvro> toConditionAvroList(List<ScenarioConditionProto> conditions) {
        return conditions.stream()
                .map(this::toConditionAvro)
                .toList();
    }

    private List<DeviceActionAvro> toActionAvroList(List<DeviceActionProto> actions) {
        return actions.stream()
                .map(this::toActionAvro)
                .toList();
    }

    private ScenarioConditionAvro toConditionAvro(ScenarioConditionProto proto) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(proto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()));

        // Обработка union-типа value {null, int, boolean}
        switch (proto.getValueCase()) {
            case BOOL_VALUE -> builder.setValue(proto.getBoolValue());
            case INT_VALUE -> builder.setValue(proto.getIntValue());
            case VALUE_NOT_SET -> builder.setValue(null);
            default -> throw new IllegalArgumentException(
                    "Unexpected value case: " + proto.getValueCase());
        }

        return builder.build();
    }

    private DeviceActionAvro toActionAvro(DeviceActionProto proto) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ActionTypeAvro.valueOf(proto.getType().name()))
                .setValue(proto.hasValue() ? proto.getValue() : null)
                .build();
    }
}