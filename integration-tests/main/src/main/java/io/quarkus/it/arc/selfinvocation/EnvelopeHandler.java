package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

public interface EnvelopeHandler<T> extends PayloadHandler<T> {

    @Override
    default void process(T payload, Instant timestamp) {
        throw new UnsupportedOperationException("Use process(Envelope) instead.");
    }

    void process(Envelope<T> envelope);
}
