package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

/**
 * Base class implementing the envelope overload. It unwraps the envelope and self-invokes the
 * payload overload that concrete subclasses override.
 * <p>
 * A top-level class (not nested in the test) so it is part of the real application bean archive,
 * which the integration test and the native image require.
 */
public abstract class AbstractEnvelopeHandler<T> implements EnvelopeHandler<T> {

    @Override
    public void process(Envelope<T> envelope) {
        process(envelope.payload(), envelope.timestamp(), envelope.id());
    }

    public void process(T payload, Instant timestamp, String correlationId) {
        process(payload, timestamp);
    }
}
