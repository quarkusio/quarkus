package io.quarkus.smallrye.faulttolerance.test.ratelimit;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.RateLimit;

@ApplicationScoped
public class RateLimitBean {
    private AtomicInteger counter = new AtomicInteger();

    @RateLimit(value = 5, window = 1, windowUnit = ChronoUnit.MINUTES)
    public int hello() {
        return counter.incrementAndGet();
    }
}
