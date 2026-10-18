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
     * The returned {@code Instant} is converted from the date-time in the default timezone. A timezone of a cron-based job
     * is not taken into account.
     * <p>
     * Unlike {@link Trigger#getPreviousFireTime()} this method always returns the same value.
     *
     * @return the time the associated trigger was fired
     */
    Instant getFireTime();

    /**
     * If the trigger represents a cron-based job with a timezone, then the returned {@code Instant} takes the timezone into
     * account.
     * <p>
     * For example, if there is a job with cron expression {@code 0 30 20 ? * * *} with timezone {@code Europe/Berlin},
     * then the return value looks like {@code 2024-07-08T18:30:00Z}. And {@link Instant#atZone(java.time.ZoneId)} for
     * {@code Europe/Berlin} would yield {@code 2024-07-08T20:30+02:00[Europe/Berlin]}.
     *
     * @return the time the action was scheduled for
     */
    Instant getScheduledFireTime();

}
