package io.quarkus.forkjoin;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class ForkJoinProdModeTestCase {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProductionModeTestsEndpoint.class, ForkJoinPoolAssertions.class)
                    .addAsResource(new StringAsset("quarkus.banner.enabled=false\nquarkus.log.level=ERROR"),
                            "application.properties"))
            .setApplicationName("prod-mode-quarkus")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true);

    @Test
    public void testForkJoinClassLoading() {
        Assertions.assertEquals(0, config.getExitCode(), config.getStartupConsoleOutput());
        Assertions.assertFalse(config.getStartupConsoleOutput().contains("FAIL"), config.getStartupConsoleOutput());
        Assertions.assertTrue(config.getStartupConsoleOutput().contains("PASS"), config.getStartupConsoleOutput());
    }
}
