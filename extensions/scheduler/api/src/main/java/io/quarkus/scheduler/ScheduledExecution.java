package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Scheduled execution metadata.
 */
public interface ScheduledExecution {

    /**
     *
     * @return the trigger that fired the event
     */
    Trigger getTrigger();

    /**
     *
     * @return the time the event was fired
     */
    Instant getFireTime();

    /**
     *
     * @return the time the action was scheduled for
     */
    Instant getScheduledFireTime();

}
