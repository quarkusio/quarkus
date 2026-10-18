package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesHealthLivenessPathBuildItem extends SimpleBuildItem {

    private final String path;

    public KubernetesHealthLivenessPathBuildItem(String path) {
        this.path = path;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
}
