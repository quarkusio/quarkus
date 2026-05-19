package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.PortBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;

public class PortConverter {

    public static Port convert(Map.Entry<String, PortConfig> e) {
        return convert(e.getValue()).withName(e.getKey()).build();
    }

    public static ContainerPort toKubeContainerPort(Map.Entry<String, PortConfig> e) {
        final var b = new ContainerPortBuilder().withName(e.getKey());
        final var port = e.getValue();
        port.containerPort().ifPresent(b::withContainerPort);
        b.withProtocol(port.protocol().name());

        return b.build();
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
