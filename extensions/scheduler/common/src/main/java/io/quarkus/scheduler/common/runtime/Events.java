package io.quarkus.scheduler.common.runtime;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.DelayedExecution;
import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.ScheduledJobPaused;
import io.quarkus.scheduler.ScheduledJobResumed;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.scheduler.SuccessfulExecution;

public final class Events {

    private static final Logger LOG = Logger.getLogger(Events.class);

    public final Event<SkippedExecution> skippedExecution;
    public final Event<SuccessfulExecution> successExecution;
    public final Event<FailedExecution> failedExecution;
    public final Event<DelayedExecution> delayedExecution;
    public final Event<SchedulerPaused> schedulerPaused;
    public final Event<SchedulerResumed> schedulerResumed;
    public final Event<ScheduledJobPaused> scheduledJobPaused;
    public final Event<ScheduledJobResumed> scheduledJobResumed;

    public Events(Event<SkippedExecution> skippedExecution, Event<SuccessfulExecution> successExecution,
            Event<FailedExecution> failedExecution, Event<DelayedExecution> delayedExecution,
            Event<SchedulerPaused> schedulerPaused, Event<SchedulerResumed> schedulerResumed,
            Event<ScheduledJobPaused> scheduledJobPaused, Event<ScheduledJobResumed> scheduledJobResumed) {
        super();
        this.skippedExecution = skippedExecution;
        this.successExecution = successExecution;
        this.failedExecution = failedExecution;
        this.delayedExecution = delayedExecution;
        this.schedulerPaused = schedulerPaused;
        this.schedulerResumed = schedulerResumed;
        this.scheduledJobPaused = scheduledJobPaused;
        this.scheduledJobResumed = scheduledJobResumed;
    }

    public void fireSchedulerPaused() {
        fire(schedulerPaused, SchedulerPaused.INSTANCE);
    }

    public void fireSchedulerResumed() {
        fire(schedulerResumed, SchedulerResumed.INSTANCE);
    }

    public void fireScheduledJobPaused(ScheduledJobPaused payload) {
        fire(scheduledJobPaused, payload);
    }

    public void fireScheduledJobResumed(ScheduledJobResumed payload) {
        fire(scheduledJobResumed, payload);
    }

    /**
     * Fires the CDI event synchronously and asynchronously.
     * <p>
     * An exception thrown from the notification of synchronous observers is not re-thrown.
     *
     * @param <E>
     * @param event
     * @param payload
     * @return the completion stage from the asynchronous notification
     */
    public static <E> CompletionStage<E> fire(Event<E> event, E payload) {
        Objects.requireNonNull(payload);
        CompletionStage<E> cs = event.fireAsync(payload);
        try {
            event.fire(payload);
        } catch (Exception e) {
            // Intentionally do no re-throw the exception
            LOG.warnf("Error occurred while notifying observers of %s: %s", payload.getClass().getName(), e.getMessage());
        }
        return cs;
    }
}
