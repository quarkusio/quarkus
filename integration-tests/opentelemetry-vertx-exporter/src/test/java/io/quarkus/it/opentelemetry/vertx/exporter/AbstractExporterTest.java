package io.quarkus.it.opentelemetry.vertx.exporter;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.semconv.ResourceAttributes;

public abstract class AbstractExporterTest {

    Traces traces;
    Metrics metrics;
    Logs logs;

    @BeforeEach
    @AfterEach
    void setUp() {
        traces.reset();
        metrics.reset();
        logs.reset();
    }

    @Test
    void test() {
        verifyHttpResponse();
        verifyTraces();
        verifyMetrics();
        verifyLogs();
    }

    private void verifyHttpResponse() {
        when()
                .get("/hello")
                .then()
                .statusCode(200);
    }

    private void verifyTraces() {
        await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSize(1));

        ExportTraceServiceRequest request = traces.getTraceRequests().get(0);
        assertThat(request.getResourceSpansCount()).isEqualTo(1);

        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertThat(resourceSpans.getResource().getAttributesList())
                .contains(
                        KeyValue.newBuilder()
                                .setKey(SERVICE_NAME.getKey())
                                .setValue(AnyValue.newBuilder()
                                        .setStringValue("integration test").build())
                                .build())
                .contains(
                        KeyValue.newBuilder()
                                .setKey(ResourceAttributes.WEBENGINE_NAME.getKey())
                                .setValue(AnyValue.newBuilder()
                                        .setStringValue("Quarkus").build())
                                .build());
        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);
        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);
        assertThat(scopeSpans.getSpansCount()).isEqualTo(1);
        Span span = scopeSpans.getSpans(0);
        assertThat(span.getName()).isEqualTo("GET /hello");
        assertThat(span.getAttributesList())
                .contains(
                        KeyValue.newBuilder()
                                .setKey("http.request.method")
                                .setValue(AnyValue.newBuilder()
                                        .setStringValue("GET").build())
                                .build());
    }

    private void verifyMetrics() {
        Metric customMetric = getMetric("hello");
        assertThat(customMetric.getDataCase()).isEqualTo(Metric.DataCase.SUM);
        Sum sum = customMetric.getSum();
        assertThat(sum.getAggregationTemporality())
                .isEqualTo(AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE);
        assertThat(sum.getDataPointsCount()).isEqualTo(1);

        NumberDataPoint dataPoint = sum.getDataPoints(0);
        assertThat(dataPoint.getAsInt()).isEqualTo(1);
        assertThat(dataPoint.getAttributesList())
                .isEqualTo(
                        Collections.singletonList(
                                KeyValue.newBuilder()
                                        .setKey("key")
                                        .setValue(AnyValue.newBuilder().setStringValue("value").build())
                                        .build()));

        Metric requestDuration = getMetric("http.server.request.duration");
        assertThat(requestDuration.getDataCase()).isEqualTo(Metric.DataCase.HISTOGRAM);
        requestDuration.getHistogram().getDataPointsList().stream()
                .forEach(point -> {
                    assertThat(point.getCount()).isGreaterThan(0);
                });

        Metric jvmMetric = getMetric("jvm.class.loaded");
        assertThat(jvmMetric.getDataCase()).isEqualTo(Metric.DataCase.SUM);
        assertThat(jvmMetric.getSum().getDataPointsCount()).isGreaterThan(0);
    }

    private Metric getMetric(final String metricName) {
        await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    List<ExportMetricsServiceRequest> reqs = metrics.getMetricRequests();
                    Optional<Metric> metric = getMetric(metricName, reqs);
                    assertThat(metric).isPresent();
                });

        final List<ExportMetricsServiceRequest> metricRequests = metrics.getMetricRequests();
        return getMetric(metricName, metricRequests).get();
    }

    private Optional<Metric> getMetric(String metricName, List<ExportMetricsServiceRequest> metricRequests) {
        return metricRequests.stream()
                .flatMap(reqs -> reqs.getResourceMetricsList().stream())
                .flatMap(resourceMetrics -> resourceMetrics.getScopeMetricsList().stream())
                .flatMap(libraryMetrics -> libraryMetrics.getMetricsList().stream())
                .filter(metric -> metric.getName().equals(metricName))
                .findFirst();
    }

    private void verifyLogs() {
        List<ExportLogsServiceRequest> logsRequests = logs.getLogsRequests();
        await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(logsRequests).hasSizeGreaterThan(2));
        ExportLogsServiceRequest request = logsRequests.get(logsRequests.size() - 2);
        assertEquals(1, request.getResourceLogsCount());

        ResourceLogs resourceLogs = request.getResourceLogs(0);
        assertThat(resourceLogs.getResource().getAttributesList())
                .contains(
                        KeyValue.newBuilder()
                                .setKey(SERVICE_NAME.getKey())
                                .setValue(AnyValue.newBuilder().setStringValue("integration test").build())
                                .build());
        assertThat(resourceLogs.getScopeLogsCount()).isEqualTo(1);

        List<LogRecord> list = logsRequests.stream()
                .flatMap(req -> req.getResourceLogsList().stream().flatMap(res -> res.getScopeLogsList().stream()))
                .flatMap(scopeLogs -> scopeLogs.getLogRecordsList().stream())
                .toList();

        Optional<LogRecord> helloLog = list.stream()
                .filter(logRecord -> logRecord.getBody().getStringValue().contains("Hello World"))
                .findFirst();
        assertThat(helloLog).isPresent();
        assertThat(helloLog.get().getBody().getStringValue()).isEqualTo("Hello World");
        assertThat(helloLog.get().getSeverityNumber()).isEqualTo(SeverityNumber.SEVERITY_NUMBER_INFO);
    }
}
