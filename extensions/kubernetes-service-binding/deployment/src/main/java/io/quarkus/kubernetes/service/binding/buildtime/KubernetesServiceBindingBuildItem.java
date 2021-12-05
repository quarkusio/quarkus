package io.quarkus.kubernetes.service.binding.buildtime;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesServiceBindingBuildItem extends MultiBuildItem {

    private final String apiVersion;
    private final String kind;
    private final String name;

    public KubernetesServiceBindingBuildItem(String apiVersion, String kind, String name) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.name = name;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
