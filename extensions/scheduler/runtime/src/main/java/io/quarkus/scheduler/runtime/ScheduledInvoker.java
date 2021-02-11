package io.quarkus.scheduler.runtime;

import io.quarkus.arc.runtime.BeanInvoker;
import io.quarkus.scheduler.ScheduledExecution;

/**
 * Invokes a scheduled business method of a bean.
 */
public interface ScheduledInvoker extends BeanInvoker<ScheduledExecution> {

}
