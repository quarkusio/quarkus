
package io.quarkus.container.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FallbackContainerImageRegistryBuildItem extends SimpleBuildItem {

    private final String registry;

    public FallbackContainerImageRegistryBuildItem(String registry) {
        this.registry = registry;
    }

    public String getRegistry() {
        return registry;
    }
}
