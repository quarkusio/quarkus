
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesDeploymentClusterBuildItem extends MultiBuildItem {

    private final String kind;

    public KubernetesDeploymentClusterBuildItem(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }
}
