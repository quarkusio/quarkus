package io.quarkus.scheduler.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;

/**
 * An extension that provides an implementation of {@link Scheduler} must produce this build item.
 * <p>
 * If multiple extensions produce this build item with the same {@link #implementation} value then the build fails.
 */
public final class SchedulerImplementationBuildItem extends MultiBuildItem {

    private final String implementation;

    private final DotName schedulerBeanClass;

    private final int priority;

    public SchedulerImplementationBuildItem(String implementation, DotName schedulerBeanClass, int priority) {
        this.implementation = implementation;
        this.schedulerBeanClass = schedulerBeanClass;
        this.priority = priority;
    }

    public String getImplementation() {
        return implementation;
    }

    public DotName getSchedulerBeanClass() {
        return schedulerBeanClass;
    }

    /**
     * The implementation with highest priority is selected if {@link Scheduled#AUTO} is used.
     *
     * @return the priority
     * @see Scheduled#AUTO
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "SchedulerImplementationBuildItem [" + (implementation != null ? "implementation=" + implementation + ", " : "")
                + "priority=" + priority + "]";
    }

}
