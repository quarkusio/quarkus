package io.quarkus.it.arc.selfinvocation;

import java.time.Instant;

public interface PayloadHandler<T> {

    void process(T payload, Instant timestamp);

    Class<T> payloadType();
}
