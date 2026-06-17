package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.test.QuarkusExtensionTest;

public class LookupUnlessPropertyRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Service.class,
                            ServiceAlpha.class, ServiceBravo.class))
            .overrideConfigKey("service.version", "2.0.1");

    @Inject
    Client client;

    @Test
    public void testRegexMatchSuppresses() {
        // ServiceAlpha uses regex "^2\\..*" which matches "2.0.1" -> suppressed (unless = exclude on match)
        assertThat(client.alphaAvailable()).isFalse();
    }

    @Test
    public void testRegexNoMatchNotSuppressed() {
        // ServiceBravo uses regex "^3\\..*" which does not match "2.0.1" -> NOT suppressed
        assertThat(client.bravoAvailable()).isTrue();
        assertThat(client.bravo().ping()).isEqualTo("bravo");
    }

    @Singleton
    static class Client {

        @Inject
        Instance<ServiceAlpha> alphaInstance;

        @Inject
        Instance<ServiceBravo> bravoInstance;

        boolean alphaAvailable() {
            return alphaInstance.isResolvable();
        }

        boolean bravoAvailable() {
            return bravoInstance.isResolvable();
        }

        ServiceBravo bravo() {
            return bravoInstance.get();
        }
    }

    interface Service {

        String ping();
    }

    // regex matches "2.0.1" -> suppressed
    @LookupUnlessProperty(name = "service.version", stringValue = "^2\\..*", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    // regex does NOT match "2.0.1" -> NOT suppressed
    @LookupUnlessProperty(name = "service.version", stringValue = "^3\\..*", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }
}
