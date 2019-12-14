package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesImageBuildItem extends SimpleBuildItem {

    private final String imageName;

    public KubernetesImageBuildItem(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
