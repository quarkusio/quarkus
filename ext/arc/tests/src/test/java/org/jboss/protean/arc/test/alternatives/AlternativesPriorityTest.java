package org.jboss.protean.arc.test.alternatives;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class AlternativesPriorityTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Charlie.class);

    @SuppressWarnings("serial")
    @Test
    public void testAlternativePriority() {
        assertEquals(Charlie.class.getName(), Arc.container().instance(new TypeLiteral<Supplier<String>>() {
        }).get().get());
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

}
