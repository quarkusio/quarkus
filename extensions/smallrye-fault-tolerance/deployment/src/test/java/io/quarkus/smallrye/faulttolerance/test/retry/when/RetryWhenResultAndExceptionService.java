package io.quarkus.smallrye.faulttolerance.test.retry.when;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;

@ApplicationScoped
public class RetryWhenResultAndExceptionService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry
    @RetryWhen(result = IsNull.class, exception = IsIllegalArgumentException.class)
    public String hello() {
        int current = attempts.incrementAndGet();
        if (current == 1) {
            return null;
        } else if (current == 2) {
            throw new IllegalArgumentException();
        } else {
            return "hello";
        }
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
