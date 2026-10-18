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
        List<?> ports = (List<?>) definitionMap.get("ports");
        if (ports == null) {
            return Collections.emptyList();
        }
        return ports.stream().map(port -> {
            String portString;
            if (port instanceof String) {
                // Short syntax: "8080:8080" or "8080"
                portString = (String) port;
            } else if (port instanceof Map<?, ?> portMap) {
                // Long syntax:
                // ports:
                //  - name: web
                //    target: 80
                //    host_ip: 127.0.0.1
                //    published: "8080"
                //    protocol: tcp
                //    app_protocol: http
                //    mode: host
                Object target = portMap.get("target");
                Object published = portMap.get("published");
                Object hostIp = portMap.get("host_ip");
                Object protocol = portMap.get("protocol");

                StringBuilder sb = new StringBuilder();

                // host IP "127.0.0.1:"
                if (hostIp != null) {
                    sb.append(hostIp).append(":");
                }

                // published port "127.0.0.1:8080:"
                if (published != null) {
                    String publishedStr = String.valueOf(published);
                    // Port ranges "8083-9000"
                    if (publishedStr.contains("-")) {
                        publishedStr = publishedStr.split("-")[0];
                    }
                    sb.append(publishedStr).append(":");
                }

                // target port "127.0.0.1:8080:80"
                sb.append(target);

                // protocol "127.0.0.1:8080:80/tcp"
                if (protocol != null) {
                    sb.append("/").append(protocol);
                }

                portString = sb.toString();
            } else {
                throw new IllegalArgumentException("Unsupported port format: " + port);
            }
            return PortBinding.parse(portString);
        })
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
