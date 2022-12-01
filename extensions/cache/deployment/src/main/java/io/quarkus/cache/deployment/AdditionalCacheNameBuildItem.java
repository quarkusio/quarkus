package io.quarkus.cache.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to ensure that a cache of the specified name is created at runtime
 * This is used in order to create caches when means other than the standard cache annotations are used
 */
public final class AdditionalCacheNameBuildItem extends MultiBuildItem {

    private final String name;

    public AdditionalCacheNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
