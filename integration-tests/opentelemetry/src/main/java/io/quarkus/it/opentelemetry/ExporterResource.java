package io.quarkus.it.opentelemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;

@Path("")
public class ExporterResource {
    @Inject
    InMemorySpanExporter inMemorySpanExporter;
    @Inject
    InMemoryMetricExporter inMemoryMetricExporter;
    @Inject
    InMemoryLogRecordExporter inMemoryLogRecordExporter;

    @GET
    @Path("/reset")
    public Response reset() {
        inMemorySpanExporter.reset();
        inMemoryMetricExporter.reset();
        inMemoryLogRecordExporter.reset();
        return Response.ok().build();
    }

    /**
     * Will exclude export endpoint related traces
     */
    @GET
    @Path("/export")
    public List<SpanData> exportTraces() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .collect(Collectors.toList());
    }

    /**
     * Export metrics with optional filtering by name and target
     */
    @GET
    @Path("/export/metrics")
    public List<MetricData> exportMetrics(@QueryParam("name") String name, @QueryParam("target") String target) {
        return Collections.unmodifiableList(new ArrayList<>(
                inMemoryMetricExporter.getFinishedMetricItems().stream()
                        .filter(metricData -> name == null ? true : metricData.getName().equals(name))
                        .filter(metricData -> target == null ? true
                                : metricData.getData()
                                        .getPoints().stream()
                                        .anyMatch(point -> isPathFound(target, point.getAttributes())))
                        .collect(Collectors.toList())));
    }

    /**
     * Will exclude Quarkus startup logs
     */
    @GET
    @Path("/export/logs")
    public List<LogRecordData> exportLogs(@QueryParam("body") String message) {
        if (message == null) {
            return inMemoryLogRecordExporter.getFinishedLogRecordItems().stream()
                    .collect(Collectors.toList());
        }
        return inMemoryLogRecordExporter.getFinishedLogRecordItems().stream()
                .filter(logRecordData -> logRecordData.getBody().asString().equals(message))
                .collect(Collectors.toList());
    }

    private static boolean isPathFound(String path, Attributes attributes) {
        if (path == null) {
            return true;// any match
        }
        Object value = attributes.asMap().get(AttributeKey.stringKey(SemanticAttributes.HTTP_ROUTE.getKey()));
        if (value == null) {
            return false;
        }
        return value.toString().equals(path);
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    @ApplicationScoped
    static class InMemoryMetricExporterProducer {
        @Produces
        @Singleton
        InMemoryMetricExporter inMemoryMetricsExporter() {
            return InMemoryMetricExporter.create();
        }
    }

    @ApplicationScoped
    static class InMemoryLogRecordExporterProducer {
        @Produces
        @Singleton
        public InMemoryLogRecordExporter createInMemoryExporter() {
            return InMemoryLogRecordExporter.create();
        }
    }
}
