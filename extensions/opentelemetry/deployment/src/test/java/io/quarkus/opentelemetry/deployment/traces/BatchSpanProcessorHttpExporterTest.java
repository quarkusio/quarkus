package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxHttpSpanExporter;
import io.quarkus.test.QuarkusExtensionTest;

public class BatchSpanProcessorHttpExporterTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class))
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50ms")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void batchSpanProcessorHasCorrectSpanExporter() throws Exception {
        BatchSpanProcessor bsp = TestUtil.getBatchSpanProcessor(openTelemetry);
        assertNotNull(bsp, "BatchSpanProcessor should be present");

        SpanExporter spanExporter = bsp.getSpanExporter();
        assertInstanceOf(VertxHttpSpanExporter.class, spanExporter,
                "SpanExporter should be a VertxHttpSpanExporter when protocol is http/protobuf");
    }
}
