package io.quarkus.arc.test.name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NameResolutionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Bravo.class, Alpha.class);

    @Test
    public void testBeanNames() {
        assertTrue(Arc.container().instance("A").isAvailable());
        assertTrue(Arc.container().instance("bravo").isAvailable());
        assertEquals(12345, Arc.container().instance("bongo").get());
        assertEquals("bing", Arc.container().instance("producedBing").get());
        assertEquals(1, Arc.container().beanManager().getBeans("bongo").size());
        // Test that for defaulted name the @Named qualifier is replaced the defaulted value
        assertEquals("bing", Arc.container().instance(String.class, NamedLiteral.of("producedBing")).get());
    }

    @Named("A")
    @Singleton
    static class Alpha {

    }

    @Named
    @Dependent
    static class Bravo {

        @Named // -> defaulted to "producedBing"
        @Produces
        String producedBing = "bing";

        @Named // -> defaulted to "bongo"
        @Produces
        Integer getBongo() {
            return 12345;
        }

    }

}
