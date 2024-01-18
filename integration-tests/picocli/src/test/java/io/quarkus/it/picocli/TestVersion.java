package io.quarkus.it.picocli;

import static io.quarkus.it.picocli.TestUtils.createConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestVersion {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("version-app", EntryWithVersionCommand.class,
            VersionProvider.class)
            .overrideConfigKey("some.version", "1.1")
            .setCommandLineParameters("--version");

    @Test
    public void simpleTest() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("1.1");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

}
