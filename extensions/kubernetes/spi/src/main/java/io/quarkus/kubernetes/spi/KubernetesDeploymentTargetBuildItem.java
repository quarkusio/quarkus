
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesDeploymentTargetBuildItem extends MultiBuildItem {

    private final String name;
    private final String kind;

    public KubernetesDeploymentTargetBuildItem(String name, String kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return this.name;
    }

    public String getKind() {
        return this.kind;
    }
}
