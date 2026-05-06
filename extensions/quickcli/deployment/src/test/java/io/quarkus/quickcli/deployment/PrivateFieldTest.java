package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that the Quarkus bytecode transformation properly handles private fields.
 * The PrivateFieldTestCommand has private @Option and @Parameters fields
 * without setters — the deployment processor should:
 * 1. Remove ACC_PRIVATE from those fields
 * 2. Replace _quickcli_set_* placeholder methods with putfield instructions
 */
public class PrivateFieldTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("private-field-app",
            PrivateFieldTestCommand.class)
            .setCommandLineParameters("--name", "Secret", "greet");

    @Test
    public void testPrivateFieldsWork() {
        assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("greet: Secret");
        assertThat(config.getExitCode()).isZero();
    }
}
