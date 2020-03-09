package io.quarkus.commandmode;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class JavaMainTestCase {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClasses(JavaMain.class, HelloWorldNonDefault.class))
            .setApplicationName("run-exit")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertTrue(config.getStartupConsoleOutput().contains("Hello Non Default"));
        Assertions.assertEquals(20, config.getExitCode());
    }

}
