package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesLabelBuildItem extends MultiBuildItem {

    private final String key;
    private final String value;
    private final String target;

    public KubernetesLabelBuildItem(String key, String value, String target) {
        this.key = key;
        this.value = value;
        this.target = target;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getTarget() {
        return this.target;
    }
}
