package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.test.QuarkusExtensionTest;

public class LookupMixedMatchConditionsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(AlphaService.class, BravoService.class, CharlieService.class))
            .overrideConfigKey("service.enabled", "true")
            .overrideConfigKey("service.version", "1.5.3");

    @Inject
    Instance<AlphaService> alpha;

    @Inject
    Instance<BravoService> bravo;

    @Inject
    Instance<CharlieService> charlie;

    @Test
    public void testBothConditionsPass() {
        // EQ matches "true", REGEX matches "1.5.3" -> both pass, NOT suppressed
        assertThat(alpha.isResolvable()).isTrue();
        assertThat(alpha.get().ping()).isEqualTo("alpha");
    }

    @Test
    public void testEqPassesRegexFails() {
        // EQ matches "true", REGEX "^2\\..*" does NOT match "1.5.3" -> suppressed
        assertThat(bravo.isResolvable()).isFalse();
    }

    @Test
    public void testEqFailsRegexPasses() {
        // EQ "false" does NOT match "true", REGEX matches "1.5.3" -> suppressed
        assertThat(charlie.isResolvable()).isFalse();
    }

    @LookupIfProperty(name = "service.enabled", stringValue = "true")
    @LookupIfProperty(name = "service.version", stringValue = "^1\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class AlphaService {
        public String ping() {
            return "alpha";
        }
    }

    @LookupIfProperty(name = "service.enabled", stringValue = "true")
    @LookupIfProperty(name = "service.version", stringValue = "^2\\..*", match = StringValueMatch.REGEX)
    @Singleton
    static class BravoService {
        public String ping() {
            return "bravo";
        }
    }

    @LookupIfProperty(name = "service.enabled", stringValue = "false")
    @LookupIfProperty(name = "service.version", stringValue = "^1\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class CharlieService {
        public String ping() {
            return "charlie";
        }
    }
}
