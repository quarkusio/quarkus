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
        port.path().ifPresent(b::withPath);
        port.hostPort().ifPresent(b::withHostPort);
        port.containerPort().ifPresent(b::withContainerPort);
        b.withProtocol(port.protocol());
        return b;
    }
}
