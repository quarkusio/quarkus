package io.quarkus.arc.test.alternatives;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AlternativesTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class, Charlie.class);

    @SuppressWarnings("serial")
    @Test
    public void testAlternative() {
        assertEquals(Bravo.class.getName(), Arc.container().instance(new TypeLiteral<Supplier<String>>() {
        }).get().get());
    }

    @Test
    public void testAlternativeInjection() {
        assertEquals(Bravo.class.getName(), Arc.container().instance(Charlie.class).get().getSuppliedValue());
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

    @Singleton
    static class Charlie {

        @Inject
        Supplier<String> supplier;

        public String getSuppliedValue() {
            return supplier.get();
        }

    }

}
