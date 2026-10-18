package io.quarkus.arc.test.producer.staticProducers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Tests static method/field producers on a `@Dependent` bean
 */
public class DependentStaticProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyProducer.class);

    @Test
    public void testStaticProducers() {
        assertEquals(0, MyProducer.creationCount);
        assertEquals(0, MyProducer.destructionCount);

        // method producers
        InstanceHandle<Long> longMethod = Arc.container().instance(Long.class);
        assertTrue(longMethod.isAvailable());
        assertEquals(42L, longMethod.get());

        // field producers
        InstanceHandle<String> stringField = Arc.container().instance(String.class);
        assertTrue(stringField.isAvailable());
        assertEquals("foobar", stringField.get());

        assertEquals(0, MyProducer.creationCount);
        assertEquals(0, MyProducer.destructionCount);
    }

    @Dependent
    static class MyProducer {
        static int creationCount = 0;
        static int destructionCount = 0;

        @Produces
        static Long produceLong() {
            return 42L;
        }

        @Produces
        static String stringField = "foobar";

        @PostConstruct
        void create() {
            creationCount++;
        }

        @PreDestroy
        void destroy() {
            destructionCount++;
        }
    }
}
