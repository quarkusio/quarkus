package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a single @Command class is automatically detected as the top command
 * (auto-applied @TopCommand) and the app runs successfully.
 */
public class SingleCommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("single-cmd-app", HelloCommand.class)
            .setCommandLineParameters("--name", "Quarkus");

    @Test
    public void testSingleCommandExecution() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello Quarkus!");
        assertThat(config.getExitCode()).isZero();
    }
}
