package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SkippedExecution;

/**
 * A scheduled invoker wrapper that skips the execution if the predicate evaluates to true.
 *
 * @see Scheduled#skipExecutionIf()
 */
public final class SkipPredicateInvoker extends DelegateInvoker {

    private static final Logger LOG = Logger.getLogger(SkipPredicateInvoker.class);

    private final Scheduled.SkipPredicate predicate;
    private final Event<SkippedExecution> event;

    public SkipPredicateInvoker(ScheduledInvoker delegate, Scheduled.SkipPredicate predicate,
            Event<SkippedExecution> event) {
        super(delegate);
        this.predicate = predicate;
        this.event = event;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        if (predicate.test(execution)) {
            LOG.debugf("Skipped scheduled invoker execution: %s", delegate.getClass().getName());
            SkippedExecution payload = new SkippedExecution(execution,
                    predicate.getClass().getName());
            event.fire(payload);
            event.fireAsync(payload);
            return CompletableFuture.completedStage(null);
        } else {
            return delegate.invoke(execution);
        }
    }

}
