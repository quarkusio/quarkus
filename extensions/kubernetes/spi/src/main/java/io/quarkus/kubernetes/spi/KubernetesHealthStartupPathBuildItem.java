package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesHealthStartupPathBuildItem extends SimpleBuildItem {

    private final String path;

    public KubernetesHealthStartupPathBuildItem(String path) {
        this.path = path;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
}
