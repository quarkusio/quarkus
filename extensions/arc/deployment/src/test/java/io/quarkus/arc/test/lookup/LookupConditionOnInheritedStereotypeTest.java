package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupConditionOnInheritedStereotypeTest {
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
    @Inherited
    @interface AlphaStereotype {
    }

    @AlphaStereotype
    static class AlphaServiceParent {
    }

    @Singleton
    static class AlphaService extends AlphaServiceParent {
        public String ping() {
            return "alpha";
        }
    }

    @Stereotype
    @LookupIfProperty(name = "service.bravo.enabled", stringValue = "true")
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface BravoStereotype {
    }

    @BravoStereotype
    static class BravoServiceParent {
    }

    @Singleton
    static class BravoService extends BravoServiceParent {
        public String ping() {
            return "bravo";
        }
    }

    @Stereotype
    @LookupIfProperty(name = "service.charlie.enabled", stringValue = "true", lookupIfMissing = true)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface CharlieStereotype {
    }

    @CharlieStereotype
    static class CharlieServiceParent {
    }

    @Singleton
    static class CharlieService extends CharlieServiceParent {
        public String ping() {
            return "charlie";
        }
    }
}
