package io.quarkus.it.kafka.streams;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * A {@link RequestScoped} bean that delegates to {@link CdiProcessorTracker}.
 * <p>
 * If CDI request context is not active when this bean's methods are invoked,
 * a {@code ContextNotActiveException} will be thrown. This proves that
 * {@code CdiAwareProcessorSupplier} correctly activates request context
 * around each {@code process()} call.
 */
@RequestScoped
public class RequestScopedHandler {

    @Inject
    CdiProcessorTracker tracker;

    public void handle(String value) {
        tracker.track(value);
    }
}
