
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesEnvBuildItem extends MultiBuildItem {

    private final String name;
    private final String value;
    private final String secret;
    private final String configmap;
    private final String field;
    private final String target;

    public KubernetesEnvBuildItem(String name, String value, String target) {
        this(name, value, null, null, null, target);
    }

    public KubernetesEnvBuildItem(String name, String value, String secret, String configmap, String field, String target) {
        this.name = name;
        this.value = value;
        this.secret = secret;
        this.configmap = configmap;
        this.field = field;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSecret() {
        return secret;
    }

    public String getConfigmap() {
        return configmap;
    }

    public String getField() {
        return field;
    }

    public String getTarget() {
        return target;
    }

}
