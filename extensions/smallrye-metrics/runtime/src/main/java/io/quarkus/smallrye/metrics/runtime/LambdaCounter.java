package io.quarkus.smallrye.metrics.runtime;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.metrics.Counter;

public class LambdaCounter implements Counter {

    private static final String MUST_NOT_BE_CALLED = "Must not be called";

    public LambdaCounter(Callable<Long> callable) {
        this.callable = callable;
    }

    @Override
    public void inc() {
        throw new IllegalStateException(MUST_NOT_BE_CALLED);
    }

    @Override
    public void inc(long n) {
        throw new IllegalStateException(MUST_NOT_BE_CALLED);
    }

    @Override
    public long getCount() {
        try {
            return this.callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private final Callable<Long> callable;
}
