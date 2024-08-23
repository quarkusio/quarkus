package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker item to specify that SnapStart is enabled.
 */
public final class SnapStartEnabledBuildItem extends SimpleBuildItem {
    public static final SnapStartEnabledBuildItem INSTANCE = new SnapStartEnabledBuildItem();

    private SnapStartEnabledBuildItem() {
        // Avoid direct instantiation.
    }
}
