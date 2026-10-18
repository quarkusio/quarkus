package io.quarkus.arc.test.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusExtensionTest;

public class UnlessBuildPropertyRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Service.class,
                            ServiceAlpha.class, ServiceBravo.class))
            .overrideConfigKey("build.mode", "staging");

    @Inject
    Client client;

    @Test
    public void testRegexMatchDisables() {
        // ServiceAlpha uses regex "stag.*" which matches "staging" -> disabled/vetoed
        assertThat(client.alphaAvailable()).isFalse();
    }

    @Test
    public void testRegexNoMatchEnables() {
        // ServiceBravo uses regex "^prod.*" which does not match "staging" -> enabled
        assertThat(client.bravoAvailable()).isTrue();
        assertThat(client.bravo().ping()).isEqualTo("bravo");
    }

    @ApplicationScoped
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

    // regex matches "staging" -> disabled
    @UnlessBuildProperty(name = "build.mode", stringValue = "stag.*", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    // regex does NOT match "staging" -> enabled
    @UnlessBuildProperty(name = "build.mode", stringValue = "^prod.*", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }
}
