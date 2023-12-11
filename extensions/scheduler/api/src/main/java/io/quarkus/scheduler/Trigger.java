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
     *
     * @return the next time at which the trigger is scheduled to fire, or {@code null} if it will not fire again
     */
    Instant getNextFireTime();

    /**
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
