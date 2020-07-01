package io.quarkus.scheduler.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;

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

    public SkipConcurrentExecutionInvoker(ScheduledInvoker delegate) {
        this.running = new AtomicBoolean(false);
        this.delegate = delegate;
    }

    @Override
    public void invoke(ScheduledExecution execution) {
        if (running.compareAndSet(false, true)) {
            try {
                delegate.invoke(execution);
            } finally {
                running.set(false);
            }
        } else {
            LOGGER.debugf("Skipped scheduled invoker execution: %s", delegate.getClass().getName());
        }
    }

    @Override
    public void invokeBean(ScheduledExecution param) {
        throw new UnsupportedOperationException();
    }

}
