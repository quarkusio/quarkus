package io.quarkus.deployment.logging;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Indicates that the static init logging setup has been done.
 * <p>
 * See {@link LoggingSetupBuildItem} when you want the full logging subsystem to be properly initialized with runtime
 * configuration.
 */
public final class StaticInitLoggingSetupBuildItem extends SimpleBuildItem {
}
