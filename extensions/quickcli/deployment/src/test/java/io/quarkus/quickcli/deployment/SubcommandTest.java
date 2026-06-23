package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests subcommand routing with a @TopCommand parent.
 */
public class SubcommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("subcommand-app",
            TopEntryCommand.class, GoodbyeCommand.class)
            .setCommandLineParameters("goodbye");

    @Test
    public void testSubcommandExecution() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Goodbye was requested!");
        assertThat(config.getExitCode()).isZero();
    }
}
