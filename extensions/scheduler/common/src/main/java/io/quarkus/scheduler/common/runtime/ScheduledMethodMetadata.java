package io.quarkus.scheduler.common.runtime;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.scheduler.Scheduled;

/**
 * Build-time metadata of a {@code @Scheduled} business method, turned into an {@link ImmutableScheduledMethod} at runtime.
 */
public record ScheduledMethodMetadata(String invokerClassName, String declaringClassName, String methodName,
        List<ScheduledMetadata> schedules) {

    public ScheduledMethod toScheduledMethod() {
        List<Scheduled> scheduled = new ArrayList<>(schedules.size());
        for (ScheduledMetadata schedule : schedules) {
            scheduled.add(schedule.toScheduled());
        }
        return new ImmutableScheduledMethod(invokerClassName, declaringClassName, methodName, scheduled);
    }
}
