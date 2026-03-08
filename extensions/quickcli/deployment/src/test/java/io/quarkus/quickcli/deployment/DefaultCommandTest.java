package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a command works with default option values when no args are provided.
 */
public class DefaultCommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("default-app", HelloCommand.class);

    @Test
    public void testDefaultValues() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello World!");
        assertThat(config.getExitCode()).isZero();
    }
}
