package io.quarkus.stork.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item used as a marker to indicate that Stork initialization has been recorded.
 * <p>
 * Its presence ensures that subsequent build steps depending on an initialized Stork (e.g. service instance registration)
 * are executed in the correct order.
 */
public final class StorkInitializedBuildItem extends SimpleBuildItem {
}
