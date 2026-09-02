package ru.yandex.practicum.aggregator.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DiscoveryDebugService {

    private final DiscoveryClient discoveryClient;

    public DiscoveryDebugService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public List<ServiceInstanceInfo> findInstances(String serviceId) {
        return discoveryClient.getInstances(serviceId).stream()
                .map(this::toInfo)
                .toList();
    }

    private ServiceInstanceInfo toInfo(ServiceInstance instance) {
        return new ServiceInstanceInfo(
                instance.getServiceId(),
                instance.getHost(),
                instance.getPort(),
                instance.getUri().toString(),
                instance.getMetadata()
        );
    }

    public static class ServiceInstanceInfo {

        private final String serviceId;
        private final String host;
        private final int port;
        private final String uri;
        private final Map<String, String> metadata;

        public ServiceInstanceInfo(
                String serviceId,
                String host,
                int port,
                String uri,
                Map<String, String> metadata
        ) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
            this.uri = uri;
            this.metadata = metadata;
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUri() {
            return uri;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}