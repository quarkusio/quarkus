package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxGrpcSpanExporter;
import io.quarkus.test.QuarkusUnitTest;

public class LoggingAndCdiSpanExporterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class))
            .overrideConfigKey("quarkus.otel.traces.exporter", "logging,cdi")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50ms");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void hasSimpleSpanProcessorForLoggingAndBspForCdi() throws Exception {
        List<SpanProcessor> processors = TestUtil.getSpanProcessors(openTelemetry);

        boolean hasSimple = processors.stream().anyMatch(p -> p instanceof SimpleSpanProcessor);
        boolean hasBatch = processors.stream().anyMatch(p -> p instanceof BatchSpanProcessor);

        assertTrue(hasSimple,
                "Should have a SimpleSpanProcessor for logging. Found: " + processorNames(processors));
        assertTrue(hasBatch,
                "Should have a BatchSpanProcessor for cdi. Found: " + processorNames(processors));
    }

    @Test
    void bothLoggingAndVertxGrpcExportersAreUsed() throws Exception {
        List<SpanExporter> exporters = TestUtil.getAllSpanExporters(openTelemetry);

        boolean hasLogging = exporters.stream()
                .anyMatch(e -> e instanceof LoggingSpanExporter);
        boolean hasVertxGrpc = exporters.stream()
                .anyMatch(e -> e instanceof VertxGrpcSpanExporter);

        assertTrue(hasLogging,
                "LoggingSpanExporter should be present. Found exporters: " + exporterNames(exporters));
        assertTrue(hasVertxGrpc,
                "VertxGrpcSpanExporter should be present. Found exporters: " + exporterNames(exporters));
        assertEquals(2, exporters.size(),
                "There should be exactly 2 span exporters. Found: " + exporterNames(exporters));
    }

    private static List<String> exporterNames(List<SpanExporter> exporters) {
        return exporters.stream().map(e -> e.getClass().getSimpleName()).toList();
    }

    private static List<String> processorNames(List<SpanProcessor> processors) {
        return processors.stream().map(p -> p.getClass().getSimpleName()).toList();
    }
}
