package io.quarkus.smallrye.context.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item for build ordering. Signifies that CP is set up
 * and ready for use.
 */
public final class ContextPropagationInitializedBuildItem extends SimpleBuildItem {

}
