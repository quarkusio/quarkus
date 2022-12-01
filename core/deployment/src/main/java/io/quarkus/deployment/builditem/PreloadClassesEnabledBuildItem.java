package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Extension build steps can produce this if preloading classes is enabled
 */
public final class PreloadClassesEnabledBuildItem extends SimpleBuildItem {
    private final boolean initialize;

    public PreloadClassesEnabledBuildItem(boolean initialize) {
        this.initialize = initialize;
    }

    public boolean doInitialize() {
        return initialize;
    }
}
