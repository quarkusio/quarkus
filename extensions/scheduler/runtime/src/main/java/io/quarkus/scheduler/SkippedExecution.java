package io.quarkus.scheduler;

import java.time.Instant;

/**
 * A CDI event that is fired synchronously and asynchronously when a concurrent execution of a scheduled method is skipped.
 * 
 * @see io.quarkus.scheduler.Scheduled.ConcurrentExecution#SKIP
 */
public class SkippedExecution {

    public final String triggerId;

    public final Instant fireTime;

    public SkippedExecution(String triggerId, Instant fireTime) {
        this.triggerId = triggerId;
        this.fireTime = fireTime;
    }

}
