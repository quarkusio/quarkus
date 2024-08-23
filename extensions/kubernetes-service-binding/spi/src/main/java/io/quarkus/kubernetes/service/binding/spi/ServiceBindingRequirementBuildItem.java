package io.quarkus.kubernetes.service.binding.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that describes a kubernetes resource that the application needs to bind to.
 */
public final class ServiceBindingRequirementBuildItem extends MultiBuildItem {

    private final String binding;
    private final String apiVersion;
    private final String kind;
    private final String name;

    public ServiceBindingRequirementBuildItem(String binding, String apiVersion, String kind, String name) {
        this.binding = binding;
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.name = name;
    }

    public String getBinding() {
        return this.binding;
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
