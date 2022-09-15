package io.quarkus.arc.test.alternatives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AlternativesPriorityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Charlie.class,
            Consumer.class);

    @SuppressWarnings("serial")
    @Test
    public void testAlternativePriority() {
        assertEquals(Charlie.class.getName(), Arc.container().instance(new TypeLiteral<Supplier<String>>() {
        }).get().get());

        assertEquals(Charlie.class.getName(), Arc.container().instance(Consumer.class).get().consume());
    }

    @Singleton
    static class Alpha implements Supplier<String> {

        @Override
        public String get() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(1)
    @Singleton
    static class Bravo implements Supplier<String> {

        @Override
        public String get() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(10)
    @Singleton
    static class Charlie {

        @Produces
        Supplier<String> supplier = () -> Charlie.class.getName();

    }

    @Singleton
    static class Consumer {
        @Inject
        Supplier<String> supplier;

        public String consume() {
            return supplier.get();
        }
    }

}
