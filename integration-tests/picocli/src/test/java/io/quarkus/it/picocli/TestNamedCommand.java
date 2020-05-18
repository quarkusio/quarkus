package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestNamedCommand {
    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("named-app", EntryCommand.class, NamedCommand.class)
            .overrideConfigKey("quarkus.picocli.top-command", "PicocliEntry");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("NamedCommand called!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

}
