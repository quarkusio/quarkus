package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Trigger is bound to a scheduled job.
 * <p>
 * It represents the logic that is used to test if a scheduled job should be executed
 * at a specific time, i.e. the trigger is "fired".
 *
 * @see Scheduled
 */
public interface Trigger {

    /**
     *
     * @return the identifier of the job
     * @see Scheduled#identity()
     * @see Scheduler#newJob(String)
     */
    String getId();

    /**
     * If the trigger represents a cron-based job with a timezone, then the returned {@code Instant} takes the timezone into
     * account.
     * <p>
     * For example, if there is a job with cron expression {@code 0 30 20 ? * * *} with timezone {@code Europe/Berlin}, then the
     * return value looks like {@code 2024-07-08T18:30:00Z}. And {@link Instant#atZone(java.time.ZoneId)} for
     * {@code Europe/Berlin} would yield {@code 2024-07-08T20:30+02:00[Europe/Berlin]}.
     *
     * @return the next time at which the trigger is scheduled to fire, or {@code null} if it will not fire again
     */
    Instant getNextFireTime();

    /**
     * If the trigger represents a cron-based job with a timezone, then the returned {@code Instant} takes the timezone into
     * account.
     * <p>
     * For example, if there is a job with cron expression {@code 0 30 20 ? * * *} with timezone {@code Europe/Berlin}, then the
     * return value looks like {@code 2024-07-08T18:30:00Z}. And {@link Instant#atZone(java.time.ZoneId)} for
     * {@code Europe/Berlin} would yield {@code 2024-07-08T20:30+02:00[Europe/Berlin]}.
     *
     * @return the previous time at which the trigger fired, or {@code null} if it has not fired yet
     */
    Instant getPreviousFireTime();

    /**
     * The grace period is configurable with {@link Scheduled#overdueGracePeriod()}.
     * <p>
     * Skipped executions are not considered as overdue.
     *
     * @return {@code false} if the last execution took place between the expected execution time and the end of the grace
     *         period, {@code true} otherwise
     * @see Scheduled#overdueGracePeriod()
     */
    boolean isOverdue();

    /**
     *
     * @return the method description or {@code null} for a trigger of a programmatically added job
     */
    default String getMethodDescription() {
        return null;
    }

}
