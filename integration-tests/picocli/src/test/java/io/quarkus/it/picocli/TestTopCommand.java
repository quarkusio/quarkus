package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestTopCommand {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("goodbye-app", GoodbyeCommand.class, EntryCommand.class)
            .setCommandLineParameters("goodbye");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Goodbye was requested!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

}
