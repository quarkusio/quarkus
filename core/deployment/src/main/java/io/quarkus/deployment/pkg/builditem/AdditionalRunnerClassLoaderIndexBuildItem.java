package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker BuildItem to ensure that {@code io.quarkus.deployment.pkg.steps.PreBootBuildStep#preBootFastJar}
 * is actually executed
 */
public final class AdditionalRunnerClassLoaderIndexBuildItem extends SimpleBuildItem {
}
