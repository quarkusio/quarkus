package io.quarkus.infinispan.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents the values of the {@link io.quarkus.infinispan.client.InfinispanClientName}.
 */
public final class InfinispanClientNameBuildItem extends MultiBuildItem {

    private final String name;

    public InfinispanClientNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
