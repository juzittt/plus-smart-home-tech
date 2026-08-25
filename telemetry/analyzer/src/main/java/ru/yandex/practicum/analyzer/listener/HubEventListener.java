package ru.yandex.practicum.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventListener {

    private final HubEventService hubEventService;

    @KafkaListener(
            topics = "${kafka.topics.hubs}",
            groupId = "${kafka.consumer.hub.group-id}",
            containerFactory = "hubEventKafkaListenerContainerFactory"
    )
    public void handleHubEvent(HubEventAvro event) {
        try {
            String eventType = getEventTypeName(event);
            log.debug("Received hub event: type={}, hubId={}",
                    eventType, event.getHubId());

            hubEventService.handleEvent(event);

            log.debug("Hub event processed: type={}, hubId={}",
                    eventType, event.getHubId());

        } catch (Exception e) {
            log.error("Error processing hub event for hubId={}: {}",
                    event.getHubId(), e.getMessage(), e);
        }
    }


    private String getEventTypeName(HubEventAvro event) {
        return switch (event.getPayload()) {
            case DeviceAddedEventAvro ignored ->
                    "DEVICE_ADDED";
            case DeviceRemovedEventAvro ignored ->
                    "DEVICE_REMOVED";
            case ScenarioAddedEventAvro ignored ->
                    "SCENARIO_ADDED";
            case ScenarioRemovedEventAvro ignored ->
                    "SCENARIO_REMOVED";
            case null, default -> "UNKNOWN";
        };
    }
}
