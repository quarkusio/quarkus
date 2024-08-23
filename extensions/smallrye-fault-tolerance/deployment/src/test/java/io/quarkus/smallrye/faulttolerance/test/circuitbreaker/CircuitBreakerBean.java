package io.quarkus.smallrye.faulttolerance.test.circuitbreaker;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class CircuitBreakerBean {
    private final AtomicBoolean shouldFail = new AtomicBoolean();

    @CircuitBreaker(requestVolumeThreshold = 5)
    @CircuitBreakerName("my-cb")
    public void hello() {
        if (!shouldFail.getAndSet(true)) {
            return;
        }
        throw new RuntimeException();
    }
}
