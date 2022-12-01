package io.quarkus.smallrye.metrics.runtime;

import org.eclipse.microprofile.metrics.Counter;

/**
 * A helper abstract class for implementing counters which only need a getCount method.
 * Other methods throw an exception.
 */
public abstract class GetCountOnlyCounter implements Counter {

    private static final String MUST_NOT_BE_CALLED = "Must not be called";

    @Override
    public void inc() {
        throw new IllegalStateException(MUST_NOT_BE_CALLED);
    }

    @Override
    public void inc(long n) {
        throw new IllegalStateException(MUST_NOT_BE_CALLED);
    }

    @Override
    public abstract long getCount();
}
