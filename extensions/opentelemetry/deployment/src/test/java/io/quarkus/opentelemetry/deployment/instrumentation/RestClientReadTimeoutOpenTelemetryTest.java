package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.SemconvResolver;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/52239">#52239</a>.
 */
public class RestClientReadTimeoutOpenTelemetryTest {

    static final AtomicReference<String> TRACE_ID_AFTER_SLEEP = new AtomicReference<>();
    static final CountDownLatch SLOW_HANDLER_DONE = new CountDownLatch(1);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addPackage(TestSpanExporter.class.getPackage())
            .addClasses(SemconvResolver.class)
            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider"))
            .withConfigurationResource("application-default.properties")
            .overrideConfigKey("quarkus.log.console.format",
                    "%d{HH:mm:ss} %-5p traceId=%X{traceId}, spanId=%X{spanId} [%c{2.}] (%t) %s%e%n")
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.opentelemetry\".level", "DEBUG")
            .overrideConfigKey("quarkus.rest-client.slow-client.url", "${test.url}")
            .overrideConfigKey("quarkus.rest-client.slow-client.read-timeout", "3000");
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RestClientReadTimeoutOpenTelemetryTest.class);

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        TRACE_ID_AFTER_SLEEP.set(null);
    }

    @Test
    void readTimeoutDoesNotLoseServerOtelContext() throws InterruptedException {
        given().get("/caller").then().statusCode(200);

        // Wait for the slow handler to complete (it sleeps 1s, timeout is 0.5s)
        SLOW_HANDLER_DONE.await(3, SECONDS);

        // Wait for spans to be exported. We expect at least:
        //   1. server span for GET /caller
        //   2. server span for GET /slow (ended when connection was reset)
        List<SpanData> spans = spanExporter.getFinishedSpanItemsAtLeast(2);

        // Verify exactly one server span for /slow (no duplicates from double sendResponse)
        long slowServerSpanCount = spans.stream()
                .filter(s -> s.getKind() == SERVER && s.getName().contains("slow"))
                .count();
        assertEquals(1, slowServerSpanCount, "Expected exactly one server span for /slow, got: " + spans);

        // Verify the server span for /slow has an error (connection closed)
        SpanData slowServerSpan = spans.stream()
                .filter(s -> s.getKind() == SERVER && s.getName().contains("slow"))
                .findFirst()
                .orElseThrow();
        assertEquals(StatusCode.ERROR, slowServerSpan.getStatus().getStatusCode());

        // Verify the traceId was still valid after the sleep (the core assertion for #52239)
        String traceIdAfterSleep = TRACE_ID_AFTER_SLEEP.get();
        assertNotEquals(null, traceIdAfterSleep, "Slow handler did not capture traceId after sleep");
        assertNotEquals("00000000000000000000000000000000", traceIdAfterSleep,
                "OTel context was lost: Span.current() returned invalid traceId after client disconnect");

        // Verify the traceId matches the exported server span
        assertEquals(slowServerSpan.getTraceId(), traceIdAfterSleep,
                "TraceId after sleep should match the exported server span's traceId");
    }

    @Path("/caller")
    public static class CallerResource {
        private static final Logger logger = Logger.getLogger(CallerResource.class);

        @Inject
        @RestClient
        SlowClient slowClient;

        @GET
        public String call() {
            try {
                logger.infov("Calling Slow");
                String slow = slowClient.slow();
                logger.infov("client received: {0}", slow);
                return slow;
            } catch (Exception e) {
                return "timeout";
            }
        }
    }

    @RegisterRestClient(configKey = "slow-client")
    @Path("/slow")
    public interface SlowClient {
        @GET
        String slow();
    }

    @Path("/slow")
    public static class SlowResource {
        private static final Logger logger = Logger.getLogger(SlowResource.class);

        @GET
        public String slow() throws InterruptedException {
            logger.infov("Span before sleep: {0}", OpenTelemetryUtil.getSpanData(Context.current()));

            try {
                // Sleep longer than the client read timeout (1s) to trigger a connection reset
                Thread.sleep(5000);
                // Capture the traceId AFTER the client has disconnected
                TRACE_ID_AFTER_SLEEP.set(Span.current().getSpanContext().getTraceId());
                logger.infov("Span after sleep: {0}", OpenTelemetryUtil.getSpanData(Context.current()));
                return "slow response";
            } finally {
                SLOW_HANDLER_DONE.countDown();
            }
        }
    }
}
