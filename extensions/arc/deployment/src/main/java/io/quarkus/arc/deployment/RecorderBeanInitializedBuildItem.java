package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker build item to indicate that a build step initializes a CDI bean "manually" through a
 * {@link io.quarkus.runtime.annotations.Recorder}
 *
 * @deprecated use synthetic beans for bean initialization instead
 */
@Deprecated
public final class RecorderBeanInitializedBuildItem extends MultiBuildItem {
}
