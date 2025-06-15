package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker build item to indicate that a build step initializes a CDI bean "manually" through a
 * {@link io.quarkus.runtime.annotations.Recorder}.
 * <p>
 * A build step does not necessarily need to create an instance of this build item, declaring a
 * {@code BuildProducer<RecorderBeanInitializedBuildItem>} parameter is enough.
 * <p>
 * If a build step consumes a {@code List<RecorderBeanInitializedBuildItem>} parameter then it will be executed after
 * all build steps that produce this build item.
 * <p>
 * This build item is deprecated because initialization of a bean via a recorder method is considered a bad practice.
 * Extension authors are encouraged to use {@link SyntheticBeanBuildItem} instead. See
 * https://github.com/quarkusio/quarkus/issues/24441 for more information.
 *
 * @deprecated use synthetic beans for bean initialization instead
 */
@Deprecated
public final class RecorderBeanInitializedBuildItem extends MultiBuildItem {
}
