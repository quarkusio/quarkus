package io.quarkus.smallrye.faulttolerance.test.retry.stackoverflow;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryStackOverflowService {
    @Retry(maxRetries = 10_000, jitter = 0)
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        throw new RuntimeException("trigger retry");
    }

    public String fallback() {
        return "fallback";
    }
}
