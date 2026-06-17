package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.test.QuarkusExtensionTest;

public class LookupIfPropertyRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Service.class,
                            ServiceAlpha.class, ServiceBravo.class,
                            ServiceCharlie.class, ServiceDelta.class))
            .overrideConfigKey("service.version", "1.5.3");

    @Inject
    Client client;

    @Test
    public void testRegexMatch() {
        // ServiceAlpha uses regex "^1\\.\\d+\\.\\d+$" which matches "1.5.3" -> NOT suppressed
        assertThat(client.alphaAvailable()).isTrue();
        assertThat(client.alpha().ping()).isEqualTo("alpha");
    }

    @Test
    public void testRegexNoMatch() {
        // ServiceBravo uses regex "^2\\.\\d+\\.\\d+$" which does not match "1.5.3" -> suppressed
        assertThat(client.bravoAvailable()).isFalse();
    }

    @Test
    public void testRegexPropertyMissing() {
        // ServiceCharlie checks a property that doesn't exist, lookupIfMissing=false -> suppressed
        assertThat(client.charlieAvailable()).isFalse();
    }

    @Test
    public void testRegexPropertyMissingWithLookupIfMissing() {
        // ServiceDelta checks a property that doesn't exist, lookupIfMissing=true -> NOT suppressed
        assertThat(client.deltaAvailable()).isTrue();
        assertThat(client.delta().ping()).isEqualTo("delta");
    }

    @Singleton
    static class Client {

        @Inject
        Instance<ServiceAlpha> alphaInstance;

        @Inject
        Instance<ServiceBravo> bravoInstance;

        @Inject
        Instance<ServiceCharlie> charlieInstance;

        @Inject
        Instance<ServiceDelta> deltaInstance;

        boolean alphaAvailable() {
            return alphaInstance.isResolvable();
        }

        ServiceAlpha alpha() {
            return alphaInstance.get();
        }

        boolean bravoAvailable() {
            return bravoInstance.isResolvable();
        }

        boolean charlieAvailable() {
            return charlieInstance.isResolvable();
        }

        boolean deltaAvailable() {
            return deltaInstance.isResolvable();
        }

        ServiceDelta delta() {
            return deltaInstance.get();
        }
    }

    interface Service {

        String ping();
    }

    // regex matches "1.5.3" -> NOT suppressed
    @LookupIfProperty(name = "service.version", stringValue = "^1\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    // regex does NOT match "1.5.3" -> suppressed
    @LookupIfProperty(name = "service.version", stringValue = "^2\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }

    // property "service.nonexistent" missing, lookupIfMissing=false -> suppressed
    @LookupIfProperty(name = "service.nonexistent", stringValue = ".*", match = StringValueMatch.REGEX)
    @Dependent
    static class ServiceCharlie implements Service {

        public String ping() {
            return "charlie";
        }
    }

    // property "service.nonexistent2" missing, lookupIfMissing=true -> NOT suppressed
    @LookupIfProperty(name = "service.nonexistent2", stringValue = ".*", match = StringValueMatch.REGEX, lookupIfMissing = true)
    @Singleton
    static class ServiceDelta implements Service {

        public String ping() {
            return "delta";
        }
    }
}
