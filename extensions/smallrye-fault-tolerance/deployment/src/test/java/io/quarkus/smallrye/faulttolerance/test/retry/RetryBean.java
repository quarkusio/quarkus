package io.quarkus.smallrye.faulttolerance.test.retry;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryBean {
    private AtomicInteger counter = new AtomicInteger();

    @Retry
    public int hello() {
        int inc = counter.incrementAndGet();
        if (inc <= 3) {
            throw new RuntimeException("should retry");
        }
        return inc;
    }
}
