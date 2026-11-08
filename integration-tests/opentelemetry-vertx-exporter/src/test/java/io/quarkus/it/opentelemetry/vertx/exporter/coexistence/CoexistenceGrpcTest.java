package io.quarkus.it.opentelemetry.vertx.exporter.coexistence;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.trace.v1.Span;
import io.quarkus.it.opentelemetry.vertx.exporter.Logs;
import io.quarkus.it.opentelemetry.vertx.exporter.Metrics;
import io.quarkus.it.opentelemetry.vertx.exporter.OtelCollectorLifecycleManager;
import io.quarkus.it.opentelemetry.vertx.exporter.Traces;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * End-to-end test proving coexistence: with the flag on, a single request produces telemetry that
 * reaches both the built-in OTLP exporter (received by the collector) and a custom CDI exporter
 * (captured in-application), for traces, metrics and logs.
 */
@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, initArgs = @ResourceArg(name = "protocol", value = "grpc"), restrictToAnnotatedClass = true)
@TestProfile(CoexistenceProfile.class)
public class CoexistenceGrpcTest {

    // Injected by OtelCollectorLifecycleManager.
    Traces traces;
    Metrics metrics;
    Logs logs;

    @Inject
    CoexistenceProfile.CapturedTelemetry captured;

    @Test
    void builtInAndCustomExportersBothReceiveTelemetry() {
        when()
                .get("/hello")
                .then()
                .statusCode(200);

        // The built-in OTLP exporter still sends everything to the collector.
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(collectorSpanNames()).contains("GET /hello"));
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(collectorMetricNames()).contains("hello"));
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(collectorLogBodies()).contains("Hello World"));

        // The custom CDI exporters receive the same telemetry.
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(captured.getSpanCount()).isGreaterThan(0));
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(captured.getMetricNames()).contains("hello"));
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(captured.getLogBodies()).contains("Hello World"));
    }

    private List<String> collectorSpanNames() {
        return traces.getTraceRequests().stream()
                .flatMap(req -> req.getResourceSpansList().stream())
                .flatMap(resourceSpans -> resourceSpans.getScopeSpansList().stream())
                .flatMap(scopeSpans -> scopeSpans.getSpansList().stream())
                .map(Span::getName)
                .toList();
    }

    private List<String> collectorMetricNames() {
        return metrics.getMetricRequests().stream()
                .flatMap(req -> req.getResourceMetricsList().stream())
                .flatMap(resourceMetrics -> resourceMetrics.getScopeMetricsList().stream())
                .flatMap(scopeMetrics -> scopeMetrics.getMetricsList().stream())
                .map(Metric::getName)
                .toList();
    }

    private List<String> collectorLogBodies() {
        return logs.getLogsRequests().stream()
                .flatMap(req -> req.getResourceLogsList().stream())
                .flatMap(resourceLogs -> resourceLogs.getScopeLogsList().stream())
                .flatMap(scopeLogs -> scopeLogs.getLogRecordsList().stream())
                .map(logRecord -> logRecord.getBody().getStringValue())
                .toList();
    }

    @BeforeEach
    @AfterEach
    void resetTelemetry() {
        traces.reset();
        metrics.reset();
        logs.reset();
        captured.reset();
    }
}
