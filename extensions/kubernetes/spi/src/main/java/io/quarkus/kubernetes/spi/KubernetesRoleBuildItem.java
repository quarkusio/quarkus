package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesRoleBuildItem extends MultiBuildItem {

    private final String role;

    public KubernetesRoleBuildItem(String role) {
        this.role = role;
    }

    public String getRole() {
        return this.role;
    }
}
