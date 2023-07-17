package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.duplicate;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@Singleton
public class SuperCircuitBreakerService {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "super";
    }
}
