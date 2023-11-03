package io.quarkus.it.opentelemetry.reactive;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.internal.data.ExceptionEventData;

@Path("")
public class ExporterResource {
    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @GET
    @Path("/reset")
    public Response reset() {
        inMemorySpanExporter.reset();
        return Response.ok().build();
    }

    @GET
    @Path("/export")
    public List<SpanData> export() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/exportExceptionMessages")
    public List<String> exportExceptionMessages() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .filter(sd -> !sd.getEvents().isEmpty())
                .flatMap(sd -> sd.getEvents().stream())
                .filter(e -> e instanceof ExceptionEventData)
                .map(e -> (ExceptionEventData) e)
                .map(e -> e.getException().getMessage())
                .collect(Collectors.toList());
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
