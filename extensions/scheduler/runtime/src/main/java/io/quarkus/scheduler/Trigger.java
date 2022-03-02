package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Trigger is bound to a scheduled task.
 *
 * @see Scheduled
 */
public interface Trigger {

    /**
     *
     * @return the identifier
     * @see Scheduled#identity()
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
     * This method returns {@code false} if the last execution has been skipped.
     *
     * @return {@code true} if the last execution of the trigger is overdue, {@code false} otherwise
     * @see Scheduled#overdueGracePeriod()
     */
    boolean isOverdue();

}
