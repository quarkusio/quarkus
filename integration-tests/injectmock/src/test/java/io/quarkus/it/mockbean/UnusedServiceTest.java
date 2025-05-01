package io.quarkus.it.mockbean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UnusedServiceTest {

    @InjectMock
    UnusedService unusedService;

    @Test
    public void testInjectedUnusedBeanIsNotRemoved() {
        Assertions.assertNotNull(unusedService);

        Assertions.assertTrue(Arc.container().instance(UnusedService.class).isAvailable());
    }

    @Test
    public void testNonInjectedUnusedBeanIsRemoved() {
        Assertions.assertFalse(Arc.container().instance(OtherUnusedService.class).isAvailable());
    }
}
