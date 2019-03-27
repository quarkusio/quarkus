package io.quarkus.kubernetes.spi;

import org.jboss.builder.item.MultiBuildItem;

public final class KubernetesPortBuildItem extends MultiBuildItem {

    private final int port;
    private final String name;

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
