package io.quarkus.commandmode;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class SimpleCommandModeBothMainsTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(BothTypesMain.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput()).contains("STATIC MAIN");
        Assertions.assertThat(config.getStartupConsoleOutput()).contains("QUARKUS MAIN");
        Assertions.assertThat(config.getExitCode()).isEqualTo(10);
    }

}
