package ru.yandex.practicum.aggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryDebugRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryDebugRunner.class);

    private final DiscoveryDebugService discoveryDebugService;

    public DiscoveryDebugRunner(DiscoveryDebugService discoveryDebugService) {
        this.discoveryDebugService = discoveryDebugService;
    }

    @Override
    public void run(ApplicationArguments args) {
        discoveryDebugService.findInstances("collector")
                .forEach(instance -> log.warn(
                        "\nService instance:\nserviceId={},\nhost={},\nport={},\nuri={},\nmetadata={}",
                        instance.getServiceId(),
                        instance.getHost(),
                        instance.getPort(),
                        instance.getUri(),
                        instance.getMetadata()
                ));
    }
}