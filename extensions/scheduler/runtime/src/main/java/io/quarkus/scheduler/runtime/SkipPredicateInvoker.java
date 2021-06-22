package io.quarkus.scheduler.runtime;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.SkippedExecution;

/**
 * A scheduled invoker wrapper that skips the execution if the predicate evaluates to true.
 * 
 * @see Scheduled#skipExecutionIf()
 */
public final class SkipPredicateInvoker implements ScheduledInvoker {

    private static final Logger LOGGER = Logger.getLogger(SkipPredicateInvoker.class);

    private final ScheduledInvoker delegate;
    private final Scheduled.SkipPredicate predicate;
    private final Event<SkippedExecution> event;

    public SkipPredicateInvoker(ScheduledInvoker delegate, Scheduled.SkipPredicate predicate,
            Event<SkippedExecution> event) {
        this.delegate = delegate;
        this.predicate = predicate;
        this.event = event;
    }

    @Override
    public void invoke(ScheduledExecution execution) throws Exception {
        if (predicate.test(execution)) {
            LOGGER.debugf("Skipped scheduled invoker execution: %s", delegate.getClass().getName());
            SkippedExecution payload = new SkippedExecution(execution,
                    predicate.getClass().getName());
            event.fire(payload);
            event.fireAsync(payload);
        } else {
            delegate.invoke(execution);
        }
    }

    @Override
    public void invokeBean(ScheduledExecution param) {
        throw new UnsupportedOperationException();
    }

}
