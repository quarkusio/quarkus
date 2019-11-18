package io.quarkus.scheduler.runtime;

import io.quarkus.scheduler.ScheduledExecution;

/**
 * Invokes a scheduled business method of a bean.
 *
 * @author Martin Kouba
 */
public interface ScheduledInvoker {

    void invoke(ScheduledExecution execution);

}
