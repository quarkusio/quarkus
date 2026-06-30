package io.quarkus.it.vertx.nativetransport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that the application fails to start when {@code native-transport=required} and the requested native
 * transport is not available (no native transport dependency on the classpath).
 */
class NativeTransportRequiredFailureTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreetingResource.class)
                    .addAsResource("application.properties"))
            .setApplicationName("native-transport-required-failure")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true);

    @Test
    void startupShouldFailWhenRequiredTransportUnavailable() {
        assertThat(config.getStartupConsoleOutput())
                .contains("Native transport was requested but no native transport dependency was found");
        assertThat(config.getExitCode()).isNotNull().isNotEqualTo(0);
    }
}
