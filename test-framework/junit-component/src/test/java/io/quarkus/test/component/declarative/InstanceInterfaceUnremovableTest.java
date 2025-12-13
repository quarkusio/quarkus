package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class InstanceInterfaceUnremovableTest {

    @Inject
    Instance<SomeInterface> instance;

    @Test
    public void testComponents() {
        assertTrue(instance.isResolvable());
    }

    @Dependent
    public static class Foo implements SomeInterface {
    }

}
