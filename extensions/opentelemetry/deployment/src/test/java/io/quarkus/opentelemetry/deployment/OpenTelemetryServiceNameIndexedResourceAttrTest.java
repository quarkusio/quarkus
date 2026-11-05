package io.quarkus.opentelemetry.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.ResourceWarningGuard;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Same as {@link OpenTelemetryServiceNameResourceAttrTest}, but the resource attributes are written in
 * the indexed list form. SmallRye Config accepts both.
 */
public class OpenTelemetryServiceNameIndexedResourceAttrTest extends OpenTelemetryServiceNameBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = ResourceWarningGuard.apply(new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(ResourceWarningGuard.class))
            .overrideRuntimeConfigKey("quarkus.otel.bsp.schedule.delay", "50")// speed up test
            .overrideRuntimeConfigKey("quarkus.otel.resource.attributes[0]", "service.name=" + SERVICE_NAME)
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false"));
}
