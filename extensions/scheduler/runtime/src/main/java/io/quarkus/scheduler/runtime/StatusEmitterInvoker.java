package io.quarkus.scheduler.runtime;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SuccessfulExecution;

/**
 * A scheduled invoker wrapper that skips concurrent executions.
 *
 * @see Scheduled#concurrentExecution()
 * @see Scheduled.ConcurrentExecution#SKIP
 */
public final class StatusEmitterInvoker implements ScheduledInvoker {

    private static final Logger LOGGER = Logger.getLogger(StatusEmitterInvoker.class);

    private final ScheduledInvoker delegate;
    private final Event<SuccessfulExecution> successfulExecutionEvent;
    private final Event<FailedExecution> failedExecutionEvent;

    public StatusEmitterInvoker(ScheduledInvoker delegate, Event<SuccessfulExecution> successfulExecutionEvent,
            Event<FailedExecution> failedExecutionEvent) {
        this.delegate = delegate;
        this.successfulExecutionEvent = successfulExecutionEvent;
        this.failedExecutionEvent = failedExecutionEvent;
    }

    @Override
    public void invoke(ScheduledExecution execution) throws Exception {

        try {
            delegate.invoke(execution);
            SuccessfulExecution successExecution = new SuccessfulExecution(execution);
            successfulExecutionEvent.fireAsync(successExecution);
            successfulExecutionEvent.fire(successExecution);
        } catch (Throwable t) {
            LOGGER.errorf(t, "Error occured while executing task for trigger %s", execution.getTrigger());
            FailedExecution failedExecution = new FailedExecution(execution, t);
            failedExecutionEvent.fireAsync(failedExecution);
            failedExecutionEvent.fire(failedExecution);
            // rethrow for quartz job listeners
            throw t;
        }
    }

    @Override
    public void invokeBean(ScheduledExecution param) {
        throw new UnsupportedOperationException();
    }

}
