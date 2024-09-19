package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.DelayedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;

/**
 * Delays execution of a scheduled task.
 *
 * @see Scheduled#executionMaxDelay()
 */
public class DelayedExecutionInvoker extends DelegateInvoker {

    private static final Logger LOG = Logger.getLogger(DelayedExecutionInvoker.class);

    private final long maxDelay;

    private final ScheduledExecutorService executor;

    private final Event<DelayedExecution> event;

    public DelayedExecutionInvoker(ScheduledInvoker delegate, long maxDelay, ScheduledExecutorService executor,
            Event<DelayedExecution> event) {
        super(delegate);
        this.maxDelay = maxDelay;
        this.executor = executor;
        this.event = event;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        long delay = ThreadLocalRandom.current().nextLong(maxDelay);

        DelayedExecution delayedExecution = new DelayedExecution(execution, delay);
        try {
            event.fire(delayedExecution);
            event.fireAsync(delayedExecution);
        } catch (Exception e) {
            LOG.errorf("Error while firing DelayedExecution event", e);
        }

        CompletableFuture<Void> ret = new CompletableFuture<>();
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    delegate.invoke(execution);
                    ret.complete(null);
                } catch (Exception e) {
                    ret.completeExceptionally(e);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
        return ret;
    }

}
