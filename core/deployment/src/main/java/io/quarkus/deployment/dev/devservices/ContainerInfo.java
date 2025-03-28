package io.quarkus.deployment.dev.devservices;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public record ContainerInfo(String id, String[] names, String imageName, String status, Map<String, String[]> networks,
        Map<String, String> labels, ContainerPort[] exposedPorts) {

    public String getShortId() {
        return id.substring(0, 12);
    }

    public String formatNames() {
        return String.join(",", names);
    }

    public String formatNetworks() {
        return networks.entrySet().stream()
                .map(e -> {
                    String[] aliases = e.getValue();
                    if (aliases == null || aliases.length == 0) {
                        return e.getKey();
                    }
                    return e.getKey() + " (" + String.join(", ", aliases) + ")";
                }).collect(Collectors.joining(", "));
    }

    public String formatPorts() {
        return Arrays.stream(exposedPorts)
                .filter(p -> p.publicPort != null)
                .map(c -> c.ip + ":" + c.publicPort + "->" + c.privatePort + "/" + c.type)
                .collect(Collectors.joining(", "));
    }

    public record ContainerPort(String ip, Integer privatePort, Integer publicPort, String type) {

    }
}
