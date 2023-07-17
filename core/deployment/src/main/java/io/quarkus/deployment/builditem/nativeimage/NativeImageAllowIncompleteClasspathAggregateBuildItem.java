package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Do not use directly: use {@see io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathBuildItem}
 * instead.
 */
public final class NativeImageAllowIncompleteClasspathAggregateBuildItem extends SimpleBuildItem {

    private final boolean allow;

    public NativeImageAllowIncompleteClasspathAggregateBuildItem(boolean allow) {
        this.allow = allow;
    }

    public boolean isAllow() {
        return allow;
    }

}
