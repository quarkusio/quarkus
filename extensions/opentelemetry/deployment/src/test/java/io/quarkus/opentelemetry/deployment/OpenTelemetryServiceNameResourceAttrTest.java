package io.quarkus.opentelemetry.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryServiceNameResourceAttrTest extends OpenTelemetryServiceNameBaseTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class))
            .overrideConfigKey("quarkus.mongodb.devservices.enabled", "false")
            .overrideRuntimeConfigKey("quarkus.otel.bsp.schedule.delay", "50")// speed up test
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes", "service.name=" + SERVICE_NAME);
}
