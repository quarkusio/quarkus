package io.quarkus.scheduler;

import java.time.Instant;

/**
 * Trigger is bound to a scheduled business method.
 *
 * @author Martin Kouba
 * @see Scheduled
 */
public interface Trigger {

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

}
