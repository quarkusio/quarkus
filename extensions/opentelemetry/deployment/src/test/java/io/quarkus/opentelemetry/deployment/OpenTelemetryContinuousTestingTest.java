package io.quarkus.opentelemetry.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.traces.TracerRouterUT;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;

public class OpenTelemetryContinuousTestingTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TracerRouter.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties(
                            "quarkus.otel.traces.exporter=test-span-exporter",
                            "quarkus.otel.metrics.exporter=none")),
                            "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TracerRouterUT.class));

    @Test
    void testContinuousTesting() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();

        TestStatus ts = utils.waitForNextCompletion();
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        TEST.modifySourceFile(TracerRouter.class, s -> s.replace("Hello", "Goodbye"));

        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        TEST.modifyTestSourceFile(TracerRouterUT.class, s -> s.replace("Hello", "Goodbye"));

        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());
    }
}
