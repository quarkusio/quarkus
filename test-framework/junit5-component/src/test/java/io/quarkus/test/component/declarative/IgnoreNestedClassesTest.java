package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.declarative.IgnoreNestedClassesTest.Alpha;

@QuarkusComponentTest(value = Alpha.class, addNestedClassesAsComponents = false)
public class IgnoreNestedClassesTest {

    @Test
    public void testComponents() {
        assertTrue(Arc.container().instance(Alpha.class).isAvailable());
        assertFalse(Arc.container().instance(Bravo.class).isAvailable());
    }

    @Unremovable
    @Singleton
    static class Alpha {

    }

    @Unremovable
    @Singleton
    static class Bravo {

    }

}
