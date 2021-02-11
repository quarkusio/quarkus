package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker used by Build Steps that consume bootstrap configuration to ensure that they run after the bootstrap config has been
 * setup
 */
public final class BootstrapConfigSetupCompleteBuildItem extends EmptyBuildItem {
}
