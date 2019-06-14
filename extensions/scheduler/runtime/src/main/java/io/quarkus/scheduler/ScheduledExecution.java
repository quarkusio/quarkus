package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Scheduled execution metadata.
 *
 * @author Martin Kouba
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
