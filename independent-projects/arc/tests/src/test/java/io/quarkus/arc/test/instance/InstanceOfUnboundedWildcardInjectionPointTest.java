package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InstanceOfUnboundedWildcardInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, FooBar.class);

    @Test
    public void test() {
        Instance<?> instance = Arc.container().instance(Head.class).get().instance;
        // there is exactly 1 enabled alternative (so it has highest priority)
        assertTrue(instance.isResolvable());
        assertInstanceOf(FooBar.class, instance.get());
    }

    @Dependent
    static class Head {
        @Inject
        Instance<?> instance;
    }

    @Dependent
    @Alternative
    @Priority(1000)
    static class FooBar {
    }
}
