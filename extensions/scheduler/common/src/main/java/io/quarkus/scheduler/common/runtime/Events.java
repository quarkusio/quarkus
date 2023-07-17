package io.quarkus.scheduler.common.runtime;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

public final class Events {

    private static final Logger LOG = Logger.getLogger(Events.class);

    private Events() {
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
