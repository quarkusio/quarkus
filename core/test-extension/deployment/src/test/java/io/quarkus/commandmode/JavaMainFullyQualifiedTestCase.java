package io.quarkus.commandmode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class JavaMainFullyQualifiedTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(JavaMain.class, HelloWorldNonDefault.class))
            .setApplicationName("run-exit")
            .overrideConfigKey("quarkus.package.main-class", HelloWorldNonDefault.class.getName())
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput()).contains("Hello Non Default");
        Assertions.assertThat(config.getExitCode()).isEqualTo(20);
    }

}
