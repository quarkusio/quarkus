package io.quarkus.smallrye.faulttolerance.test.multiple;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;

@ApplicationScoped
public class MultipleBean {
    @Bulkhead
    @BeforeRetry(methodName = "beforeRetry")
    @CircuitBreaker
    @CircuitBreakerName("hello-cb")
    @ExponentialBackoff
    @Fallback(fallbackMethod = "fallback")
    @RateLimit
    @Retry
    @RetryWhen
    @Timeout
    public String hello() {
        throw new RuntimeException();
    }

    String fallback() {
        return "fallback";
    }

    void beforeRetry() {
    }
}
