package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Execution metadata of a specific scheduled job.
 */
public interface ScheduledExecution {

    /**
     *
     * @return the trigger that fired the execution
     */
    Trigger getTrigger();

    /**
     * Unlike {@link Trigger#getPreviousFireTime()} this method always returns the same value.
     *
     * @return the time the associated trigger was fired
     */
    Instant getFireTime();

    /**
     *
     * @return the time the action was scheduled for
     */
    Instant getScheduledFireTime();

}
