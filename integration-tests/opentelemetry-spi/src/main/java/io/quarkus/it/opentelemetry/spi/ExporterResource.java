package io.quarkus.it.opentelemetry.spi;

import java.util.ArrayList;
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
import jakarta.ws.rs.core.Response;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

@Path("")
public class ExporterResource {
    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @GET
    @Path("/reset")
    public Response reset() {
        inMemorySpanExporter.reset();
        return Response.ok().build();
    }

    @GET
    @Path("/export")
    public List<Map<String, Object>> export() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .map(ExporterResource::toSpanMap)
                .collect(Collectors.toList());
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

    @GET
    @Path("/export/propagation")
    public List<String> exportPropagation() throws NoSuchFieldException, IllegalAccessException {
        TextMapPropagator textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        return new ArrayList<>(textMapPropagator.fields());
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }
}
