package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;

public abstract class RemoveUnusedComponentsTest {

    protected void assertPresent(Class<?> beanClass) {
        assertTrue(Arc.container().instance(beanClass).isAvailable());
    }

    protected void assertNotPresent(Class<?> beanClass) {
        assertEquals(0, Arc.container().beanManager().getBeans(beanClass).size());
    }
}
