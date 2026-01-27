package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.test.QuarkusUnitTest;

public class LookupMultipleConditionsTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(AlphaService.class, BravoService.class))
            .overrideConfigKey("service.alpha.enabled", "true")
            .overrideConfigKey("service.alpha.active", "true")
            .overrideConfigKey("service.bravo.enabled", "true")
            .overrideConfigKey("service.bravo.active", "false");

    @Inject
    Instance<AlphaService> alpha;

    @Inject
    Instance<BravoService> bravo;

    @Test
    public void testConditions() {
        assertTrue(alpha.isResolvable());
        assertEquals("alpha", alpha.get().ping());
        assertTrue(bravo.isUnsatisfied());
    }

    @LookupIfProperty(name = "service.alpha.enabled", stringValue = "true")
    @LookupIfProperty(name = "service.alpha.active", stringValue = "true")
    @Singleton
    static class AlphaService {
        public String ping() {
            return "alpha";
        }
    }

    @LookupIfProperty(name = "service.bravo.enabled", stringValue = "true")
    @LookupIfProperty(name = "service.bravo.active", stringValue = "true")
    @Singleton
    static class BravoService {
        public String ping() {
            return "bravo";
        }
    }
}
