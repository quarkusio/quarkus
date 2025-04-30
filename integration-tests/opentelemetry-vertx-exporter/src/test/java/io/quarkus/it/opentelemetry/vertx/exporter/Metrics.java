package io.quarkus.it.opentelemetry.vertx.exporter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;

public final class Metrics {

    private final List<ExportMetricsServiceRequest> metricRequests = new CopyOnWriteArrayList<>();

    public List<ExportMetricsServiceRequest> getMetricRequests() {
        return metricRequests;
    }

    public void reset() {
        metricRequests.clear();
    }
}
