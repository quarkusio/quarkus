package io.quarkus.it.opentelemetry;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public List<Map<String, Object>> exportTraces() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .map(ExporterResource::toSpanMap)
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
    public List<Map<String, Object>> exportLogs(@QueryParam("body") String message) {
        var items = inMemoryLogRecordExporter.getFinishedLogRecordItems();
        if (message != null) {
            items = items.stream()
                    .filter(logRecordData -> logRecordData.getBody().asString().equals(message))
                    .collect(Collectors.toList());
        }
        return items.stream().map(ExporterResource::toLogMap).collect(Collectors.toList());
    }

    private static Map<String, Object> toSpanMap(SpanData sd) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spanId", sd.getSpanId());
        map.put("traceId", sd.getTraceId());
        map.put("name", sd.getName());
        map.put("kind", sd.getKind().name());
        map.put("ended", sd.hasEnded());
        map.put("parentSpanId", sd.getParentSpanContext().getSpanId());
        map.put("parent_spanId", sd.getParentSpanContext().getSpanId());
        map.put("parent_traceId", sd.getParentSpanContext().getTraceId());
        map.put("parent_remote", sd.getParentSpanContext().isRemote());
        map.put("parent_valid", sd.getParentSpanContext().isValid());
        sd.getAttributes().forEach((k, v) -> map.put("attr_" + k.getKey(), v.toString()));
        sd.getResource().getAttributes().forEach((k, v) -> map.put("resource_" + k.getKey(), v.toString()));
        return map;
    }

    private static Map<String, Object> toLogMap(LogRecordData lr) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("severityText", lr.getSeverityText());
        Map<String, Object> spanCtx = new LinkedHashMap<>();
        spanCtx.put("traceId", lr.getSpanContext().getTraceId());
        spanCtx.put("spanId", lr.getSpanContext().getSpanId());
        spanCtx.put("sampled", lr.getSpanContext().isSampled());
        map.put("spanContext", spanCtx);
        map.put("body_body", lr.getBody().asString());
        lr.getAttributes().forEach((k, v) -> map.put("attr_" + k.getKey(), v.toString()));
        lr.getResource().getAttributes().forEach((k, v) -> map.put("resource_" + k.getKey(), v.toString()));
        return map;
    }

    private static boolean isPathFound(String path, Attributes attributes) {
        if (path == null) {
            return true;// any match
        }
        Object value = attributes.asMap().get(AttributeKey.stringKey(HTTP_ROUTE.getKey()));
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
