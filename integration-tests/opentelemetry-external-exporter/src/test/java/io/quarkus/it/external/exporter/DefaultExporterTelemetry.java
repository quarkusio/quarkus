package io.quarkus.it.external.exporter;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import io.opentelemetry.proto.metrics.v1.Histogram;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;

public class DefaultExporterTelemetry {

    private final Traces traces;
    private final Metrics metrics;
    private final Logs logs;

    public DefaultExporterTelemetry(Traces traces, Metrics metrics, Logs logs) {
        this.traces = traces;
        this.metrics = metrics;
        this.logs = logs;
    }

    public void verifyExportedTraces() {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSize(1));

        ExportTraceServiceRequest request = traces.getTraceRequests().get(0);
        assertThat(request.getResourceSpansCount()).isEqualTo(1);

        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertThat(resourceSpans.getResource().getAttributesList())
                .contains(KeyValue.newBuilder()
                        .setKey(SERVICE_NAME.getKey())
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("opentelemetry-external-exporter-integration-test").build())
                        .build());
        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);

        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);
        assertThat(scopeSpans.getSpansCount()).isEqualTo(1);

        Span span = scopeSpans.getSpans(0);
        assertThat(span.getName()).isEqualTo("GET /hello");
        assertThat(span.getAttributesList())
                .contains(KeyValue.newBuilder()
                        .setKey("http.request.method")
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("GET").build())
                        .build());
    }

    public void verifyNoExportedTraces() {
        assertThat(traces.getTraceRequests()).isEmpty();
    }

    public void verifyExportedMetrics() {
        Metric customMetric = getMetric(HelloResource.HISTOGRAM_NAME);
        assertThat(customMetric.getDataCase()).isEqualTo(Metric.DataCase.HISTOGRAM);

        Histogram histogram = customMetric.getHistogram();
        assertThat(histogram.getAggregationTemporality())
                .isEqualTo(AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE);
        assertThat(histogram.getDataPointsCount()).isEqualTo(1);

        HistogramDataPoint dataPoint = histogram.getDataPoints(0);
        assertThat(dataPoint.getCount()).isEqualTo(1);
        assertThat(dataPoint.getSum()).isEqualTo(10);
        assertThat(dataPoint.getAttributesList())
                .isEqualTo(Collections.singletonList(KeyValue.newBuilder()
                        .setKey("key")
                        .setValue(AnyValue.newBuilder().setStringValue("value").build())
                        .build()));

        Metric requestDuration = getMetric("http.server.request.duration");
        assertThat(requestDuration.getDataCase()).isEqualTo(Metric.DataCase.HISTOGRAM);
        requestDuration.getHistogram().getDataPointsList().stream()
                .forEach(point -> {
                    assertThat(point.getCount()).isGreaterThan(0);
                });
    }

    public void verifyNoExportedMetrics() {
        List<ExportMetricsServiceRequest> reqs = metrics.getMetricRequests();
        Optional<Metric> metric = getMetric(HelloResource.HISTOGRAM_NAME, reqs);
        assertThat(metric).isNotPresent();
    }

    private Metric getMetric(final String metricName) {
        await().atMost(Duration.ofSeconds(30))
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

    public void verifyExportedLogs() {
        List<ExportLogsServiceRequest> logsRequests = logs.getLogsRequests();
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(logsRequests).hasSizeGreaterThan(2));
        ExportLogsServiceRequest request = logsRequests.get(logsRequests.size() - 2);
        assertEquals(1, request.getResourceLogsCount());

        ResourceLogs resourceLogs = request.getResourceLogs(0);
        assertThat(resourceLogs.getResource().getAttributesList())
                .contains(KeyValue.newBuilder()
                        .setKey(SERVICE_NAME.getKey())
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("opentelemetry-external-exporter-integration-test").build())
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

    public void verifyNoExportedLogs() {
        List<ExportLogsServiceRequest> logsRequests = logs.getLogsRequests();
        assertTrue(logsRequests.isEmpty());
    }
}
