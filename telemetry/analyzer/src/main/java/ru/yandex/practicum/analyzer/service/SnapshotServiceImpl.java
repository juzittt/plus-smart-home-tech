package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.entity.Scenario;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioExecutor scenarioExecutor;

    @Override
    @Transactional
    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());

        if (scenarios.isEmpty()) {
            log.debug("No scenarios in hub: {}", snapshot.getHubId());
            return;
        }

        log.debug("Checking {} scenarios for hub: {}", scenarios.size(), snapshot.getHubId());

        scenarios.stream()
                .filter(scenario -> scenarioExecutor.checkScenario(scenario, snapshot))
                .forEach(scenario -> scenarioExecutor.executeScenario(scenario));
    }
}
