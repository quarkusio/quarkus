package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.PortBuilder;

public class PortConverter {

    public static Port convert(Map.Entry<String, PortConfig> e) {
        return convert(e.getValue()).withName(e.getKey()).build();
    }

    private static PortBuilder convert(PortConfig port) {
        PortBuilder b = new PortBuilder();
        port.path().ifPresent(v -> b.withPath(v));
        port.hostPort().ifPresent(v -> b.withHostPort(v));
        port.containerPort().ifPresent(v -> b.withContainerPort(v));
        b.withProtocol(port.protocol());
        return b;
    }
}
