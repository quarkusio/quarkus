package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestTopCommandProperty {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("hello-app", HelloCommand.class, GoodbyeCommand.class)
            .overrideConfigKey("quarkus.picocli.top-command", HelloCommand.class.getName());

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Hello World!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

}
