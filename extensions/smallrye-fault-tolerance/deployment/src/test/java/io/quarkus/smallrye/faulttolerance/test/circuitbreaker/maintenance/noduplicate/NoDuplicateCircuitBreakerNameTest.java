package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.noduplicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

public class NoDuplicateCircuitBreakerNameTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CircuitBreakerService1.class, CircuitBreakerService2.class));

    @Inject
    CircuitBreakerMaintenance cb;

    @Test
    public void deploysWithoutError() {
        assertNotNull(cb);
        assertEquals(CircuitBreakerState.CLOSED, cb.currentState("hello"));
    }
}
