package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class InstanceInterfaceTest {

    @Inject
    Instance<SomeInterface> instance;

    @Test
    public void testComponents() {
        // SomeInterface is registered as component but no implementation is added automatically
        assertTrue(instance.isUnsatisfied());
    }

}
