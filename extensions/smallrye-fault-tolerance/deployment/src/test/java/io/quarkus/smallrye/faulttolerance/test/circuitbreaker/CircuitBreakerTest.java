package io.quarkus.smallrye.faulttolerance.test.circuitbreaker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

public class CircuitBreakerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CircuitBreakerBean.class));

    @Inject
    CircuitBreakerBean circuitBreaker;

    @Inject
    CircuitBreakerMaintenance circuitBreakerMaintenance;

    @Test
    public void test() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreakerMaintenance.currentState("my-cb"));
        assertDoesNotThrow(() -> circuitBreaker.hello());
        assertThrows(RuntimeException.class, () -> circuitBreaker.hello());
        assertThrows(RuntimeException.class, () -> circuitBreaker.hello());
        assertThrows(RuntimeException.class, () -> circuitBreaker.hello());
        assertThrows(RuntimeException.class, () -> circuitBreaker.hello());
        assertThrows(CircuitBreakerOpenException.class, () -> circuitBreaker.hello());
        assertEquals(CircuitBreakerState.OPEN, circuitBreakerMaintenance.currentState("my-cb"));
    }

    @Test
    public void undefinedCircuitBreaker() {
        assertThrows(IllegalArgumentException.class, () -> {
            circuitBreakerMaintenance.currentState("undefined");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            circuitBreakerMaintenance.reset("undefined");
        });
    }
}
