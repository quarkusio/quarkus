package io.quarkus.arc.test.alternatives;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.function.Supplier;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class AlternativesPriorityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Charlie.class, Consumer.class);

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
