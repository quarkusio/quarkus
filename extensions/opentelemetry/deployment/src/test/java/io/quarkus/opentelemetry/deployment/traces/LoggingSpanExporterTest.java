package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class LoggingSpanExporterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class))
            .overrideConfigKey("quarkus.otel.traces.exporter", "logging")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void loggingExporterUsesSimpleSpanProcessor() throws Exception {
        SpanProcessor activeProcessor = TestUtil.getActiveSpanProcessor(openTelemetry);
        assertInstanceOf(SimpleSpanProcessor.class, activeProcessor,
                "Logging exporter should use SimpleSpanProcessor, but was: "
                        + activeProcessor.getClass().getName());
    }

    @Test
    void loggingExporterIsUsed() throws Exception {
        List<SpanExporter> exporters = TestUtil.getAllSpanExporters(openTelemetry);
        assertEquals(1, exporters.size(), "There should be exactly one span exporter");
        assertInstanceOf(LoggingSpanExporter.class, exporters.get(0),
                "The span exporter should be a LoggingSpanExporter, but was: "
                        + exporters.get(0).getClass().getName());
    }
}
