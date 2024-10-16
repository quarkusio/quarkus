package io.quarkus.vertx.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries all types for which the {@link io.quarkus.vertx.LocalEventBusCodec} should be selected automatically.
 */
public final class LocalCodecSelectorTypesBuildItem extends SimpleBuildItem {

    private final Set<String> types;

    LocalCodecSelectorTypesBuildItem(Set<String> types) {
        this.types = types;
    }

    public Set<String> getTypes() {
        return types;
    }

}
