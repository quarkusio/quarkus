package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.exporter.otlp.FakeUpstreamOtlpMarker;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that the mere presence of a class under the io.opentelemetry.exporter.otlp
 * package prefix (as detected by OtlpExporterProcessor#isOtlpExporterPresent — simulating
 * a user who added the real io.opentelemetry:opentelemetry-exporter-otlp dependency)
 * suppresses the Quarkus default OTLP SpanExporter, distinct from CDI-bean-based or
 * ExternalOtelExporterBuildItem-based detection covered elsewhere.
 */
public class OtlpUpstreamExporterPresentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FakeUpstreamOtlpMarker.class))
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi");

    @Inject
    Instance<SpanExporter> spanExporterInstance;

    @Test
    void upstreamOtlpExporterOnClasspathSuppressesDefault() {
        assertTrue(spanExporterInstance.isUnsatisfied(),
                "Default OTLP SpanExporter should not be created when upstream OTLP exporter classes are present");
    }
}
