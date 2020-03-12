
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesEnvBuildItem extends MultiBuildItem {

    private final String target;

    private final String key;
    private final String value;

    public KubernetesEnvBuildItem(String target, String key, String value) {
        this.target = target;
        this.key = key;
        this.value = value;
    }

    public String getTarget() {
        return this.target;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
