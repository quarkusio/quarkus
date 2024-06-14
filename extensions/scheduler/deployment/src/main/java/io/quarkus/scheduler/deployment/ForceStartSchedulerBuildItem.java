package io.quarkus.scheduler.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A marker item which forces Quarkus Scheduler initialization regardless of presence of any
 * {@link io.quarkus.scheduler.Scheduled} methods.
 * <p>
 * This option is similar to using scheduler subsystem configuration option {@code quarkus.scheduler.start-mode=forced}.
 */
public final class ForceStartSchedulerBuildItem extends MultiBuildItem {
}
