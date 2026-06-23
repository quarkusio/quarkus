package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that @Option on a setter method works correctly.
 */
public class MethodOptionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("method-opt-app", MethodOptionCommand.class)
            .setCommandLineParameters("-Dfoo=bar", "-Dbaz=qux");

    @Test
    public void testMethodOptionExecution() {
        assertThat(config.getStartupConsoleOutput()).contains("foo=bar");
        assertThat(config.getStartupConsoleOutput()).contains("baz=qux");
        assertThat(config.getExitCode()).isZero();
    }
}
