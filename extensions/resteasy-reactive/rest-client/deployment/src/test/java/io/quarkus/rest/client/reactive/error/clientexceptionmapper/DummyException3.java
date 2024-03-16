package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import java.util.concurrent.atomic.AtomicInteger;

public class DummyException3 extends RuntimeException {

    static final AtomicInteger executionCount = new AtomicInteger(0);

    public DummyException3() {
        executionCount.incrementAndGet();
        setStackTrace(new StackTraceElement[0]);
    }
}
