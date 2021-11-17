package io.quarkus.kubernetes.service.binding.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that describes a service that the application needs to bind to.
 * The qualifier does not encapsulate the target service coordinates, but information that given the right context can be mapped
 * to coordinates.
 */
public final class ServiceQualifierBuildItem extends MultiBuildItem {

    private final String kind;
    private final String name;

    public ServiceQualifierBuildItem(String kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
