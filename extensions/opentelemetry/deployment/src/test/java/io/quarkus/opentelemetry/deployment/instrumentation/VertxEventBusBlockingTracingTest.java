package io.quarkus.opentelemetry.deployment.instrumentation;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;

/**
 * Tests that OTel trace context is properly propagated to blocking @ConsumeEvent handlers
 * when using fire-and-forget EventBus messages (publish/send without reply).
 * <p>
 * This covers the fix for the race condition in HandlerRegistration.dispatch() where
 * tracer.sendResponse() was called on the event loop thread before the worker thread
 * had a chance to run, removing the OTel context from Vert.x locals.
 * <p>
 * The test verifies context propagation by creating a custom span inside the blocking
 * handler using the current OTel context. If context is not propagated, the custom span
 * will have a different traceId than the HTTP server span.
 */
public class VertxEventBusBlockingTracingTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(EventConsumers.class, TestUtil.class, TestSpanExporter.class,
                            TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "200");

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void blockingConsumeEventShouldPropagateTraceContext() {
        RestAssured.when().get("/hello/blocking-publish")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("ok"));

        // Wait for spans: HTTP server + EventBus producer + EventBus consumer + custom "inside-handler" span
        List<SpanData> spans = spanExporter.getFinishedSpanItemsAtLeast(4);

        SpanData httpSpan = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No HTTP server span found. Spans: " + spans));

        String httpTraceId = httpSpan.getTraceId();

        // Find the custom span created inside the blocking handler
        SpanData handlerSpan = spans.stream()
                .filter(s -> s.getName().equals("inside-blocking-handler"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No 'inside-blocking-handler' span found. Spans: " + spans));

        // The custom span created inside the blocking handler should share the same traceId
        // as the HTTP span. Before the fix, this would be a different traceId because
        // Context.current() returned root context in the worker thread.
        assertEquals(httpTraceId, handlerSpan.getTraceId(),
                "Span created inside blocking handler should have the same traceId as the HTTP span");
    }

    @Test
    void blockingAnnotationConsumeEventShouldPropagateTraceContext() {
        RestAssured.when().get("/hello/blocking-annotation-publish")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("ok"));

        List<SpanData> spans = spanExporter.getFinishedSpanItemsAtLeast(4);

        SpanData httpSpan = spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No HTTP server span found. Spans: " + spans));

        String httpTraceId = httpSpan.getTraceId();

        SpanData handlerSpan = spans.stream()
                .filter(s -> s.getName().equals("inside-blocking-annotation-handler"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No 'inside-blocking-annotation-handler' span found. Spans: " + spans));

        assertEquals(httpTraceId, handlerSpan.getTraceId(),
                "Span created inside @Blocking handler should have the same traceId as the HTTP span");
    }

    @Singleton
    public static class EventConsumers {

        @Inject
        Tracer tracer;

        @ConsumeEvent(value = "blocking-publish", blocking = true)
        void consumeBlocking(String message) {
            // Create a span using the current OTel context.
            // Before the fix, Context.current() would return empty/root context here
            // because HandlerRegistration.dispatch() called sendResponse() (which removes
            // the OTel context from Vert.x locals) before this worker thread ran.
            Span span = tracer.spanBuilder("inside-blocking-handler").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("test.message", message);
            } finally {
                span.end();
            }
        }

        @ConsumeEvent("blocking-annotation-publish")
        @Blocking
        void consumeBlockingAnnotation(String message) {
            Span span = tracer.spanBuilder("inside-blocking-annotation-handler").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("test.message", message);
            } finally {
                span.end();
            }
        }

        void registerRoutes(@Observes Router router, EventBus eventBus) {
            router.get("/hello/blocking-publish").handler(rc -> {
                eventBus.publish("blocking-publish", "test");
                rc.end("ok");
            });

            router.get("/hello/blocking-annotation-publish").handler(rc -> {
                eventBus.publish("blocking-annotation-publish", "test");
                rc.end("ok");
            });
        }
    }
}
