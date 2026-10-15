package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

/**
 * Decorator that overrides both interface methods, including the payload overload
 * {@code process(T, Instant)} that {@link AbstractEnvelopeHandler} self-invokes.
 * <p>
 * A top-level class (not nested in the test) so it is part of the real application bean archive,
 * which the integration test and the native image require.
 */
@Decorator
@Priority(1)
@Dependent
public class LoggingDecorator<T> implements EnvelopeHandler<T> {

    private final EnvelopeHandler<T> delegate;

    @Inject
    public LoggingDecorator(@Delegate @Any EnvelopeHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Class<T> payloadType() {
        return delegate.payloadType();
    }

    @Override
    public void process(Envelope<T> envelope) {
        delegate.process(envelope);
    }

    @Override
    public void process(T payload, Instant timestamp) {
        delegate.process(payload, timestamp);
    }
}
