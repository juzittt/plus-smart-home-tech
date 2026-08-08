package ru.yandex.practicum.telemetry.collector.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.hub_event.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = ScenarioComponentMapper.class)
public interface HubEventMapper {

    @Mapping(target = "type", source = "deviceType")
    DeviceAddedEventAvro toDeviceAddedAvro(DeviceAddedEvent event);

    DeviceRemovedEventAvro toDeviceRemovedAvro(DeviceRemovedEvent event);

    @Mapping(target = "conditions", source = "conditions", qualifiedByName = "toConditionAvroList")
    @Mapping(target = "actions", source = "actions", qualifiedByName = "toActionAvroList")
    ScenarioAddedEventAvro toScenarioAddedAvro(ScenarioAddedEvent event);

    ScenarioRemovedEventAvro toScenarioRemovedAvro(ScenarioRemovedEvent event);

    @Named("toConditionAvroList")
    List<ScenarioConditionAvro> toConditionAvroList(List<ScenarioCondition> conditions);

    @Named("toActionAvroList")
    List<DeviceActionAvro> toActionAvroList(List<DeviceAction> actions);

    default HubEventAvro toAvro(HubEvent event) {
        return switch (event) {
            case DeviceAddedEvent e -> buildHubEvent(e, toDeviceAddedAvro(e));
            case DeviceRemovedEvent e -> buildHubEvent(e, toDeviceRemovedAvro(e));
            case ScenarioAddedEvent e -> buildHubEvent(e, toScenarioAddedAvro(e));
            case ScenarioRemovedEvent e -> buildHubEvent(e, toScenarioRemovedAvro(e));
            default -> throw new IllegalArgumentException(
                    "Unknown hub event type: " + event.getType());
        };
    }

    private HubEventAvro buildHubEvent(HubEvent event, Object payload) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
