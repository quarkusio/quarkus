package io.quarkus.resteasy.common.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a JAX-RS provider class.
 */
public final class ResteasyJaxrsProviderBuildItem extends MultiBuildItem {

    private final String name;

    public ResteasyJaxrsProviderBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
