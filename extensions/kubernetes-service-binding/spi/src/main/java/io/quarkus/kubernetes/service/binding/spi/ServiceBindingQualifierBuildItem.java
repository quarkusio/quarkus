package io.quarkus.kubernetes.service.binding.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that describes a service that the application needs to bind to. The qualifier does not encapsulate the
 * target service coordinates, but information that given the right context can be mapped to coordinates.
 */
public final class ServiceBindingQualifierBuildItem extends MultiBuildItem {

    private final String id;
    private final String kind;
    private final String name;

    public ServiceBindingQualifierBuildItem(String kind, String name) {
        this(kind + "-" + name, kind, name);

    }

    public ServiceBindingQualifierBuildItem(String id, String kind, String name) {
        this.id = id.replaceAll("[^a-zA-Z0-9_-]", "");
        this.kind = kind;
        this.name = name.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    public String getId() {
        return this.id;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
