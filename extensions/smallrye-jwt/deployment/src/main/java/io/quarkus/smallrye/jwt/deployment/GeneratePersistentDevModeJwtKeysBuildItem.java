package io.quarkus.smallrye.jwt.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item to enable restart-persistent jwt keys.
 */
public final class GeneratePersistentDevModeJwtKeysBuildItem extends SimpleBuildItem {
}
