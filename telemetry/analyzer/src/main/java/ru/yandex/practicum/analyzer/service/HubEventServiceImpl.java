package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.entity.*;
import ru.yandex.practicum.analyzer.model.enums.ActionType;
import ru.yandex.practicum.analyzer.model.enums.ConditionOperation;
import ru.yandex.practicum.analyzer.model.enums.ConditionType;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    @Transactional
    public void handleEvent(HubEventAvro event) {
        switch (event.getPayload()) {
            case DeviceAddedEventAvro deviceAdded ->
                handleDeviceAdded(event.getHubId(), deviceAdded);
            case DeviceRemovedEventAvro deviceRemoved ->
                handleDeviceRemoved(event.getHubId(), deviceRemoved);
            case ScenarioAddedEventAvro scenarioAdded ->
                handleScenarioAdded(event.getHubId(), scenarioAdded);
            case ScenarioRemovedEventAvro scenarioRemoved ->
                handleScenarioRemoved(event.getHubId(), scenarioRemoved);
            default -> log.warn("Unknown hub event payload: {}",
                    event.getPayload().getClass().getSimpleName());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro payload) {
        sensorRepository.findByIdAndHubId(payload.getId(), hubId)
                .ifPresentOrElse(
                        existing -> log.debug("Sensor already exists: id={}, hubId={}",
                                payload.getId(), hubId),
                        () -> {
                            sensorRepository.save(new Sensor(payload.getId(), hubId));
                            log.info("Sensor added: id={}, hubId={}", payload.getId(), hubId);
                        }
                );
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro payload) {
        sensorRepository.findByIdAndHubId(payload.getId(), hubId)
                .ifPresent(sensor -> {
                    sensorRepository.delete(sensor);
                    log.info("Sensor removed: id={}, hubId={}", payload.getId(), hubId);
                });
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro payload) {
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, payload.getName())
                .orElseGet(() -> new Scenario(hubId, payload.getName()));

        scenario.getConditions().clear();
        scenario.getActions().clear();

        payload.getConditions().forEach(conditionAvro ->
                sensorRepository.findByIdAndHubId(conditionAvro.getSensorId(), hubId)
                        .ifPresentOrElse(
                                sensor -> scenario.addCondition(new ScenarioCondition(
                                        scenario,
                                        sensor,
                                        conditionRepository.save(toCondition(conditionAvro)))),
                                () -> log.warn("Sensor not found for condition: {}",
                                        conditionAvro.getSensorId())
                        ));

        payload.getActions().forEach(actionAvro ->
                sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId)
                        .ifPresentOrElse(
                                sensor -> scenario.addAction(new ScenarioAction(
                                        scenario,
                                        sensor,
                                        actionRepository.save(toAction(actionAvro)))),
                                () -> log.warn("Sensor not found for action: {}",
                                        actionAvro.getSensorId())
                        ));

        scenarioRepository.save(scenario);
        log.info("Scenario saved: hubId={}, name={}, conditions={}, actions={}",
                hubId, payload.getName(), scenario.getConditions().size(), scenario.getActions().size());
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro payload) {
        scenarioRepository.findByHubIdAndName(hubId, payload.getName())
                .ifPresent(scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("Scenario removed: hubId={}, name={}", hubId, payload.getName());
                });
    }

    private Condition toCondition(ScenarioConditionAvro avro) {
        return new Condition(
                ConditionType.valueOf(avro.getType().name()),
                ConditionOperation.valueOf(avro.getOperation().name()),
                toInteger(avro.getValue()));
    }

    private Action toAction(DeviceActionAvro avro) {
        return new Action(
                ActionType.valueOf(avro.getType().name()),
                avro.getValue());
    }

    private Integer toInteger(Object avroValue) {
        return switch (avroValue) {
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            case null, default -> null;
        };
    }
}
