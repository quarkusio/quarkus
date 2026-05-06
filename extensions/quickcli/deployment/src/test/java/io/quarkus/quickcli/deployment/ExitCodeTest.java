package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that exit codes from Callable commands are propagated correctly.
 */
public class ExitCodeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("exitcode-app", ExitCodeTestCommand.class)
            .setCommandLineParameters("--code", "42");

    @Test
    public void testExitCode() {
        assertThat(config.getExitCode()).isEqualTo(42);
    }
}
