package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

public interface Envelope<T> {

    T payload();

    Instant timestamp();

    String id();
}
