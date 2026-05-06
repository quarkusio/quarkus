package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests the quarkus.quickcli.top-command configuration property
 * to select a specific command when multiple exist.
 */
public class TopCommandPropertyTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("top-cmd-prop-app",
            HelloCommand.class, GoodbyeCommand.class)
            .overrideConfigKey("quarkus.quickcli.top-command",
                    HelloCommand.class.getName())
            .setCommandLineParameters("--name", "Config");

    @Test
    public void testTopCommandSelectedByProperty() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello Config!");
        assertThat(config.getExitCode()).isZero();
    }
}
