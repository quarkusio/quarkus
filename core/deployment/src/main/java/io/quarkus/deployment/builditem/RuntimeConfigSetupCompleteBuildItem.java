package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker used by Build Steps that consume runtime configuration to ensure that they run after the runtime config has been setup
 */
public final class RuntimeConfigSetupCompleteBuildItem extends EmptyBuildItem {
}
