package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class InstanceComponentTest {

    @Inject
    Instance<SomeBean> instance;

    @Test
    public void testComponents() {
        assertTrue(instance.get().ping());
    }

}
