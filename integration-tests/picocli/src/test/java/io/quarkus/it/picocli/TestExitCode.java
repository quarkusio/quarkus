package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestExitCode {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("hello-app", ExitCodeCommand.class)
            .setCommandLineParameters("--code", "42");

    @Test
    public void simpleTest() {
        assertThat(config.getExitCode()).isEqualTo(42);
    }
}
