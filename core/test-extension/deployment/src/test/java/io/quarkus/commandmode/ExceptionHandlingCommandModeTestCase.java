package io.quarkus.commandmode;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class ExceptionHandlingCommandModeTestCase {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(ThrowExceptionApplicationMain.class, ThrowExceptionApplication.class))
            .setApplicationName("exception-handling")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("exception-handling").contains("Exception and exit code [1] handled by application");
        Assertions.assertThat(config.getExitCode()).isEqualTo(10);
    }
}
