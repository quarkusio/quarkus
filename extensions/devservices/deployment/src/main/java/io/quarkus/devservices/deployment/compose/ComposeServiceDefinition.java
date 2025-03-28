package io.quarkus.devservices.deployment.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;

/**
 * Represents a service definition in a docker-compose file.
 */
public class ComposeServiceDefinition {

    private final String serviceName;
    private final Map<String, ?> definitionMap;

    public ComposeServiceDefinition(String serviceName, Map<String, ?> definitionMap) {
        this.serviceName = serviceName;
        this.definitionMap = definitionMap;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getContainerName() {
        return (String) definitionMap.get("container_name");
    }

    public List<ExposedPort> getPorts() {
        List<String> ports = (List<String>) definitionMap.get("ports");
        if (ports == null) {
            return Collections.emptyList();
        }
        return ports.stream().map(PortBinding::parse)
                .map(PortBinding::getExposedPort)
                .collect(Collectors.toList());
    }

    public boolean hasHealthCheck() {
        return definitionMap.get("healthcheck") instanceof Map;
    }

    public Map<String, Object> getLabels() {
        Object labels = definitionMap.get("labels");
        if (labels instanceof List) {
            Map<String, Object> map = new HashMap<>();
            for (Object label : ((List<?>) labels)) {
                if (label instanceof String) {
                    String[] split = ((String) label).split("=");
                    map.put(split[0], split[1]);
                } else if (label instanceof Map) {
                    map.putAll((Map<String, Object>) label);
                }
            }
            return map;
        } else if (labels instanceof Map) {
            return new HashMap<>((Map<String, Object>) labels);
        }
        return Collections.emptyMap();
    }

    public List<String> getProfiles() {
        Object profiles = definitionMap.get("profiles");
        if (profiles instanceof List) {
            return (List<String>) profiles;
        }
        return Collections.emptyList();
    }
}
