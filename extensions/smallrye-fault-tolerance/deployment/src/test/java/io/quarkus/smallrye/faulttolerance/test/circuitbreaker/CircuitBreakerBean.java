package io.quarkus.smallrye.faulttolerance.test.circuitbreaker;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class CircuitBreakerBean {
    AtomicBoolean breakTheCircuit = new AtomicBoolean();

    @CircuitBreaker(requestVolumeThreshold = 5)
    @CircuitBreakerName("my-cb")
    public void breakCircuit() {
        if (!breakTheCircuit.getAndSet(true)) {
            return;
        }
        throw new RuntimeException("let's break it !");
    }
}
