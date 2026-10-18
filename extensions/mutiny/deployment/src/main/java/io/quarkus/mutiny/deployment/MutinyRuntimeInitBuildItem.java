package io.quarkus.mutiny.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item to detect when Mutiny has been initialized at runtime.
 */
public final class MutinyRuntimeInitBuildItem extends SimpleBuildItem {

}
