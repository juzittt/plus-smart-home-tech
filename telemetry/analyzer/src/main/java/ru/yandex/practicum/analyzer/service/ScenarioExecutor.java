package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.entity.Condition;
import ru.yandex.practicum.analyzer.model.entity.Scenario;
import ru.yandex.practicum.analyzer.model.enums.ConditionOperation;
import ru.yandex.practicum.analyzer.model.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutor {

    private final HubRouterClient hubRouterClient;

    public boolean checkScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> states = snapshot.getSensorsState();

        boolean result = scenario.getConditions().stream()
                .allMatch(sc -> {
                    SensorStateAvro state = states.get(sc.getSensor().getId());
                    if (state == null) {
                        log.debug("Sensor not in snapshot: {}", sc.getSensor().getId());
                        return false;
                    }
                    return checkCondition(sc.getCondition(), state.getData());
                });
        log.debug("Scenario '{}' check result: {}", scenario.getName(), result);
        return result;
    }

    public void executeScenario(Scenario scenario) {
        log.info("Executing scenario: hubId={}, name={}", scenario.getHubId(), scenario.getName());

        scenario.getActions().forEach(action ->
                hubRouterClient.sendAction(scenario.getHubId(), scenario.getName(), action));
    }

    private boolean checkCondition(Condition condition, Object sensorData) {
        return extractValue(condition.getType(), sensorData)
                .map(actual -> applyOperation(condition.getOperation(), actual, condition.getValue()))
                .orElse(false);
    }

    private boolean applyOperation(ConditionOperation operation, int actual, Integer expected) {
        if (expected == null) {
            return false;
        }
        return switch (operation) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }

    private Optional<Integer> extractValue(ConditionType type, Object data) {
        return switch (type) {
            case TEMPERATURE -> switch (data) {
                case TemperatureSensorAvro t -> Optional.of(t.getTemperatureC());
                case ClimateSensorAvro c -> Optional.of(c.getTemperatureC());
                default -> Optional.empty();
            };
            case HUMIDITY -> switch (data) {
                case ClimateSensorAvro c -> Optional.of(c.getHumidity());
                default -> Optional.empty();
            };
            case CO2LEVEL -> switch (data) {
                case ClimateSensorAvro c -> Optional.of(c.getCo2Level());
                default -> Optional.empty();
            };
            case LUMINOSITY -> switch (data) {
                case LightSensorAvro l -> Optional.of(l.getLuminosity());
                default -> Optional.empty();
            };
            case MOTION -> switch (data) {
                case MotionSensorAvro m -> Optional.of(m.getMotion() ? 1 : 0);
                default -> Optional.empty();
            };
            case SWITCH -> switch (data) {
                case SwitchSensorAvro s -> Optional.of(s.getState() ? 1 : 0);
                default -> Optional.empty();
            };
        };
    }
}
