package io.quarkus.scheduler;

import java.time.Instant;

/**
 * This event is fired synchronously and asynchronously when an execution of a scheduled method is skipped.
 * 
 * @see io.quarkus.scheduler.Scheduled.ConcurrentExecution#SKIP
 * @see io.quarkus.scheduler.Scheduled#skipExecutionIf()
 */
public class SkippedExecution {

    // Keep the fields in order to minimize the breaking changes
    public final String triggerId;
    public final Instant fireTime;

    private final ScheduledExecution execution;
    private final String detail;

    public SkippedExecution(ScheduledExecution execution) {
        this(execution, null);
    }

    public SkippedExecution(ScheduledExecution execution, String detail) {
        this.execution = execution;
        this.detail = detail;
        this.triggerId = execution.getTrigger().getId();
        this.fireTime = execution.getFireTime();
    }

    public ScheduledExecution getExecution() {
        return execution;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Skipped execution of [")
                .append(execution.getTrigger().getId())
                .append("]");
        if (detail != null) {
            builder.append(": ").append(detail);
        }
        return builder.toString();
    }

}
