package io.quarkus.opentelemetry.deployment.traces;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimpleSpanProcessorTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestUtil.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey("quarkus.otel.simple", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    TestSpanExporter testSpanExporter;

    @BeforeEach
    void setUp() {
        testSpanExporter.reset();
    }

    @Test
    void activeProcessorIsSimpleSpanProcessorWithBatchShutdown() throws Exception {
        SpanProcessor activeProcessor = TestUtil.getActiveSpanProcessor(openTelemetry);
        assertNotNull(activeProcessor);
        assertEquals(
                "SimpleSpanProcessorWithBatchShutdown",
                activeProcessor.getClass().getSimpleName(),
                "Active processor should be SimpleSpanProcessorWithBatchShutdown when quarkus.otel.simple=true");
    }

    @Test
    void delegateIsSimpleSpanProcessor() throws Exception {
        SpanProcessor activeProcessor = TestUtil.getActiveSpanProcessor(openTelemetry);
        SpanProcessor delegate = TestUtil.getSimpleSpanProcessorDelegate(activeProcessor);

        assertInstanceOf(SimpleSpanProcessor.class, delegate,
                "The delegate should be a SimpleSpanProcessor");
    }

    @Test
    void replacedBatchProcessorIsRetained() throws Exception {
        SpanProcessor activeProcessor = TestUtil.getActiveSpanProcessor(openTelemetry);
        SpanProcessor replacedBatchProcessor = TestUtil.getReplacedBatchProcessor(activeProcessor);

        assertInstanceOf(BatchSpanProcessor.class, replacedBatchProcessor,
                "The replacedBatchProcessor should be a BatchSpanProcessor");
    }

    @Test
    void spansAreExportedThroughSimpleProcessor() {
        Tracer tracer = openTelemetry.getTracer("test");

        Span span = tracer.spanBuilder("simple-test-span").startSpan();
        span.end();

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems(1);
        assertEquals(1, spans.size());
        assertEquals("simple-test-span", spans.get(0).getName());
    }

    @Test
    void multipleSpansAreExportedCorrectly() {
        Tracer tracer = openTelemetry.getTracer("test");

        for (int i = 0; i < 3; i++) {
            Span span = tracer.spanBuilder("span-" + i).startSpan();
            span.end();
        }

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems(3);
        assertEquals(3, spans.size());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    void shutdownStopsBothDelegateAndReplacedBatchProcessor() throws Exception {
        SpanProcessor activeProcessor = TestUtil.getActiveSpanProcessor(openTelemetry);
        SimpleSpanProcessor delegate = (SimpleSpanProcessor) TestUtil.getSimpleSpanProcessorDelegate(activeProcessor);
        BatchSpanProcessor replacedBatchProcessor = (BatchSpanProcessor) TestUtil.getReplacedBatchProcessor(activeProcessor);

        Tracer tracer = openTelemetry.getTracer("test");
        Span span = tracer.spanBuilder("before-shutdown").startSpan();
        span.end();
        testSpanExporter.getFinishedSpanItems(1);
        testSpanExporter.reset();

        CompletableResultCode shutdownResult = activeProcessor.shutdown();
        shutdownResult.join(5, TimeUnit.SECONDS);
        assertTrue(shutdownResult.isSuccess(), "Shutdown result should be successful");

        assertTrue(TestUtil.isShutdown(delegate), "Delegate SimpleSpanProcessor should be shut down");
        assertTrue(TestUtil.isShutdown(replacedBatchProcessor), "Replaced BatchSpanProcessor should be shut down");

        Span postShutdownSpan = tracer.spanBuilder("after-shutdown").startSpan();
        postShutdownSpan.end();

        await().during(200, MILLISECONDS).atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, testSpanExporter.getPartialFinishedSpanItems().size(),
                        "No spans should be exported after shutdown"));
    }
}
