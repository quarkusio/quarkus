package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesResourcesBuildItem extends SimpleBuildItem {

    private final String[] resourceClasses;

    public KubernetesResourcesBuildItem(String[] resourceClasses) {
        this.resourceClasses = resourceClasses;
    }

    public String[] getResourceClasses() {
        return resourceClasses;
    }
}
