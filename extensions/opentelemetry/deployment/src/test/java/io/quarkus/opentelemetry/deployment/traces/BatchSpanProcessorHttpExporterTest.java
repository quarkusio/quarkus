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
import io.quarkus.test.QuarkusUnitTest;

public class BatchSpanProcessorHttpExporterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class))
            .overrideConfigKey("quarkus.otel.exporter.otlp.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50ms");

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
