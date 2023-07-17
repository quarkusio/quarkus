package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesClientCapabilityBuildItem extends SimpleBuildItem {

    private final boolean generateRbac;

    public KubernetesClientCapabilityBuildItem(boolean generateRbac) {
        this.generateRbac = generateRbac;
    }

    public boolean isGenerateRbac() {
        return generateRbac;
    }
}
