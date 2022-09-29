package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.deployment.annotations.Consume;

/**
 * A build item for scheduling post-resume tasks.
 * Use with {@link Consume} to cause a step to run after resume.
 */
public final class PostResumeBuildItem extends EmptyBuildItem {
}
