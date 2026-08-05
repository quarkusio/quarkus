package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.inheritance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;

public class CircuitBreakerNameInheritanceTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SubCircuitBreakerService.class, SuperCircuitBreakerService.class));

    @Inject
    CircuitBreakerMaintenance cb;

    @Test
    public void deploysWithoutError() {
        assertNotNull(cb);
    }
}
