package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.duplicate;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@Singleton
@CircuitBreaker
public class SubCircuitBreakerService extends SuperCircuitBreakerService {
    @Override
    public String hello() {
        return "sub";
    }
}
