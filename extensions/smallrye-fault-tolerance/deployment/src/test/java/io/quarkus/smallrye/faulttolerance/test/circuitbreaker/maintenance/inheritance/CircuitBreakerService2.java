package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.inheritance;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@Singleton
public class CircuitBreakerService2 {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "2";
    }
}
