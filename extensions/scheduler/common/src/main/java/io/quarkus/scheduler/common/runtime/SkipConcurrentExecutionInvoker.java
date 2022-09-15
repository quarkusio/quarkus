package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SkippedExecution;

/**
 * An invoker wrapper that skips concurrent executions.
 *
 * @see Scheduled#concurrentExecution()
 * @see io.quarkus.scheduler.Scheduled.ConcurrentExecution#SKIP
 */
public final class SkipConcurrentExecutionInvoker extends DelegateInvoker {

    private static final Logger LOG = Logger.getLogger(SkipConcurrentExecutionInvoker.class);

    private final AtomicBoolean running;
    private final Event<SkippedExecution> event;

    public SkipConcurrentExecutionInvoker(ScheduledInvoker delegate, Event<SkippedExecution> event) {
        super(delegate);
        this.running = new AtomicBoolean(false);
        this.event = event;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        if (running.compareAndSet(false, true)) {
            return delegate.invoke(execution).whenComplete((r, t) -> running.set(false));
        }
        LOG.debugf("Skipped scheduled invoker execution: %s", delegate.getClass().getName());
        SkippedExecution payload = new SkippedExecution(execution,
                "The scheduled method should not be executed concurrently");
        event.fire(payload);
        event.fireAsync(payload);
        return CompletableFuture.completedStage(null);
    }

}
