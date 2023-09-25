package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.noduplicate;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

public class CircuitBreakerService2 {
    // this class is not a bean, so there's no circuit breaker and hence no duplicate circuit breaker name
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "2";
    }
}
