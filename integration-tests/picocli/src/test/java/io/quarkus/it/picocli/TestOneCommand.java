package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestOneCommand {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("hello-app", HelloCommand.class)
            .setCommandLineParameters("--name=Tester");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello Tester!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

}
