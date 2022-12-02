package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker item to specify that CRAC is enabled
 */
public final class CracEnabledBuildItem extends SimpleBuildItem {
    public static final CracEnabledBuildItem INSTANCE = new CracEnabledBuildItem();

    private CracEnabledBuildItem() {
    }
}
