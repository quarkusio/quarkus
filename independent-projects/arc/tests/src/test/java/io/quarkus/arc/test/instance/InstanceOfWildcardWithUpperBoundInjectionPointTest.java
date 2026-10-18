package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InstanceOfWildcardWithUpperBoundInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Head.class, Producers.class);

    @Test
    public void test() {
        Instance<? extends Number> instance = Arc.container().instance(Head.class).get().instance;
        assertTrue(instance.isAmbiguous());
        assertEquals(2L, instance.stream().count());
    }

    @Dependent
    static class Head {
        @Inject
        Instance<? extends Number> instance;
    }

    @Dependent
    static class Producers {
        @Produces
        @Dependent
        Integer produceInteger() {
            return 42;
        }

        @Produces
        @Dependent
        Double produceDouble() {
            return 13.0;
        }
    }
}
