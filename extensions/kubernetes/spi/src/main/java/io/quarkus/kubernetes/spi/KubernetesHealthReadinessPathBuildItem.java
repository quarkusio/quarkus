package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesHealthReadinessPathBuildItem extends SimpleBuildItem {

    private final String path;

    public KubernetesHealthReadinessPathBuildItem(String path) {
        this.path = path;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
}
