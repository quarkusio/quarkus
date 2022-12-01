package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DuplicateCircuitBreakerNameTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CircuitBreakerService1.class, CircuitBreakerService2.class))
            .assertException(e -> {
                assertEquals(DefinitionException.class, e.getClass());
                assertTrue(e.getMessage().contains("Multiple circuit breakers have the same name"));
            });

    @Test
    public void test() {
        fail();
    }
}
