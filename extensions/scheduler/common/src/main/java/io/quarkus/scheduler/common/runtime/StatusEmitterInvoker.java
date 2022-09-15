package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SuccessfulExecution;

/**
 * An invoker wrapper that fires CDI events when an execution of a scheduled method is finished.
 *
 * @see SuccessfulExecution
 * @see FailedExecution
 */
public final class StatusEmitterInvoker extends DelegateInvoker {

    private static final Logger LOG = Logger.getLogger(StatusEmitterInvoker.class);

    private final Event<SuccessfulExecution> successfulEvent;
    private final Event<FailedExecution> failedEvent;

    public StatusEmitterInvoker(ScheduledInvoker delegate, Event<SuccessfulExecution> successfulEvent,
            Event<FailedExecution> failedEvent) {
        super(delegate);
        this.successfulEvent = successfulEvent;
        this.failedEvent = failedEvent;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        return delegate.invoke(execution).whenComplete((v, t) -> {
            if (t != null) {
                LOG.errorf(t, "Error occurred while executing task for trigger %s", execution.getTrigger());
                FailedExecution failed = new FailedExecution(execution, t);
                failedEvent.fireAsync(failed);
                failedEvent.fire(failed);
            } else {
                SuccessfulExecution success = new SuccessfulExecution(execution);
                successfulEvent.fireAsync(success);
                successfulEvent.fire(success);
            }
        });
    }

}
