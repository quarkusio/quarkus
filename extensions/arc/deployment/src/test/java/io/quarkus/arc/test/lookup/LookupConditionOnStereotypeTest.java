package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupConditionOnStereotypeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(AlphaStereotype.class, AlphaService.class,
                    BravoStereotype.class, BravoService.class, CharlieStereotype.class, CharlieService.class))
            .overrideConfigKey("service.alpha.enabled", "true")
            .overrideConfigKey("service.bravo.enabled", "false");

    @Inject
    Instance<AlphaService> alpha;

    @Inject
    Instance<BravoService> bravo;

    @Inject
    Instance<CharlieService> charlie;

    @Test
    public void testConditions() {
        assertTrue(alpha.isResolvable());
        assertEquals("alpha", alpha.get().ping());
        assertTrue(bravo.isUnsatisfied());
        assertTrue(charlie.isResolvable());
        assertEquals("charlie", charlie.get().ping());
    }

    @Stereotype
    @LookupIfProperty(name = "service.alpha.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @interface AlphaStereotype {
    }

    @Singleton
    @AlphaStereotype
    static class AlphaService {
        public String ping() {
            return "alpha";
        }
    }

    @Stereotype
    @LookupIfProperty(name = "service.bravo.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @interface BravoStereotype {
    }

    @Singleton
    @BravoStereotype
    static class BravoService {
        public String ping() {
            return "bravo";
        }
    }

    @Stereotype
    @LookupIfProperty(name = "service.charlie.enabled", stringValue = "true", lookupIfMissing = true)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CharlieStereotype {
    }

    static class CharlieService {
        public String ping() {
            return "charlie";
        }
    }

    @Singleton
    static class CharlieProducer {
        @Produces
        @Singleton
        @CharlieStereotype
        public CharlieService produce() {
            return new CharlieService();
        }
    }
}
