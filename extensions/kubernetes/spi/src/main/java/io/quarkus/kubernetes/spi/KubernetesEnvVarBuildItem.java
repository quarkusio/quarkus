package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesEnvVarBuildItem extends MultiBuildItem {

    private final String name;
    private final String value;

    public KubernetesEnvVarBuildItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

}
