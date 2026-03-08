package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that CDI injection works in QuickCLI commands via the QuickCliBeansFactory.
 */
public class CdiInjectionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("cdi-app",
            InjectedCommand.class, GreetingService.class)
            .setCommandLineParameters("--greeting", "Hello");

    @Test
    public void testCdiInjection() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello from service!");
        assertThat(config.getExitCode()).isZero();
    }
}
