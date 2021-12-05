package io.quarkus.commandmode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class ExceptionThrowingCommandModeTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(ExceptionThrowingMain.class))
            .setApplicationName("exception-throwing")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("exception-throwing").contains("ArrayIndexOutOfBoundsException");
        Assertions.assertThat(config.getExitCode()).isEqualTo(1);
    }

}
