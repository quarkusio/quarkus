package io.quarkus.cache.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This build item is used to pass the full list of cache names from the validation step to the recording step.
 */
public final class CacheNamesBuildItem extends SimpleBuildItem {

    private final Set<String> names;

    public CacheNamesBuildItem(Set<String> names) {
        this.names = names;
    }

    public Set<String> getNames() {
        return names;
    }
}
