package io.quarkus.arc.test.properties;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.test.QuarkusExtensionTest;

public class IfBuildPropertyRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Service.class,
                            ServiceAlpha.class, ServiceBravo.class,
                            ServiceCharlie.class))
            .overrideConfigKey("build.version", "1.4.7");

    @Inject
    Client client;

    @Test
    public void testRegexMatch() {
        // ServiceAlpha uses regex "^1\\.(4|5|6)\\.\\d+$" which matches "1.4.7" -> enabled
        assertThat(client.alphaAvailable()).isTrue();
        assertThat(client.alpha().ping()).isEqualTo("alpha");
    }

    @Test
    public void testRegexNoMatch() {
        // ServiceBravo uses regex "^2\\.\\d+\\.\\d+$" which does not match "1.4.7" -> disabled/vetoed
        assertThat(client.bravoAvailable()).isFalse();
    }

    @Test
    public void testRegexPropertyMissing() {
        // ServiceCharlie checks missing property with enableIfMissing=true -> enabled
        assertThat(client.charlieAvailable()).isTrue();
        assertThat(client.charlie().ping()).isEqualTo("charlie");
    }

    @ApplicationScoped
    static class Client {

        @Inject
        Instance<ServiceAlpha> alphaInstance;

        @Inject
        Instance<ServiceBravo> bravoInstance;

        @Inject
        Instance<ServiceCharlie> charlieInstance;

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

        ServiceCharlie charlie() {
            return charlieInstance.get();
        }
    }

    interface Service {

        String ping();
    }

    // regex matches "1.4.7" -> enabled
    @IfBuildProperty(name = "build.version", stringValue = "^1\\.(4|5|6)\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    // regex does NOT match "1.4.7" -> disabled/vetoed
    @IfBuildProperty(name = "build.version", stringValue = "^2\\.\\d+\\.\\d+$", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }

    // property missing, enableIfMissing=true -> enabled
    @IfBuildProperty(name = "build.nonexistent", stringValue = ".*", match = StringValueMatch.REGEX, enableIfMissing = true)
    @Singleton
    static class ServiceCharlie implements Service {

        public String ping() {
            return "charlie";
        }
    }
}
