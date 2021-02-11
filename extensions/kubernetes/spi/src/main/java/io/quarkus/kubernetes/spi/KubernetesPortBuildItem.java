package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;

public final class KubernetesPortBuildItem extends MultiBuildItem {

    private final int port;
    private final String name;

    public KubernetesPortBuildItem(int port, Feature feature) {
        this(port, feature.getName());
    }

    public KubernetesPortBuildItem(int port, String name) {
        this.port = port;
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }
}
