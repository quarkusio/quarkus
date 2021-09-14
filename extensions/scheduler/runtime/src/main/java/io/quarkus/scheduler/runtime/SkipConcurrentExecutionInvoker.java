package io.quarkus.scheduler.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SkippedExecution;

/**
 * A scheduled invoker wrapper that skips concurrent executions.
 * 
 * @see Scheduled#concurrentExecution()
 * @see io.quarkus.scheduler.Scheduled.ConcurrentExecution#SKIP
 */
public final class SkipConcurrentExecutionInvoker implements ScheduledInvoker {

    private static final Logger LOGGER = Logger.getLogger(SkipConcurrentExecutionInvoker.class);

    private final AtomicBoolean running;
    private final ScheduledInvoker delegate;
    private final Event<SkippedExecution> event;

    public SkipConcurrentExecutionInvoker(ScheduledInvoker delegate, Event<SkippedExecution> event) {
        this.running = new AtomicBoolean(false);
        this.delegate = delegate;
        this.event = event;
    }

    @Override
    public void invoke(ScheduledExecution execution) throws Exception {
        if (running.compareAndSet(false, true)) {
            try {
                delegate.invoke(execution);
            } finally {
                running.set(false);
            }
        } else {
            LOGGER.debugf("Skipped scheduled invoker execution: %s", delegate.getClass().getName());
            SkippedExecution payload = new SkippedExecution(execution,
                    "The scheduled method should not be executed concurrently");
            event.fire(payload);
            event.fireAsync(payload);
        }
    }

    @Override
    public void invokeBean(ScheduledExecution param) {
        throw new UnsupportedOperationException();
    }

}
