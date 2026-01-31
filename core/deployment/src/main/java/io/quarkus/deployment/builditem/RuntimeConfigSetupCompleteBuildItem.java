package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker used by Build Steps that consume runtime configuration to ensure that they run after the runtime config has
 * been set up.
 *
 * @deprecated this build item has no effect. The runtime config is now always set up before build steps execute. Build
 *             steps consuming this annotation can safely remove it.
 */
@Deprecated(forRemoval = true, since = "3.31")
public final class RuntimeConfigSetupCompleteBuildItem extends EmptyBuildItem {
}
