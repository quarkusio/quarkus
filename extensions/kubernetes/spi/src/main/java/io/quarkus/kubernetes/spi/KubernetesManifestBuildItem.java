package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesManifestBuildItem extends MultiBuildItem {

    private final String target;
    private final String path;

    public KubernetesManifestBuildItem(String target, String path) {
        this.target = target;
        this.path = path;
    }

    public String getTarget() {
        return target;
    }

    public String getPath() {
        return path;
    }
}
