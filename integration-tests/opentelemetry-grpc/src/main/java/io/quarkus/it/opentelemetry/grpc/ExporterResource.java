package io.quarkus.it.opentelemetry.grpc;

import static java.util.Comparator.comparingLong;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

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
                .sorted(comparingLong(SpanData::getStartEpochNanos).reversed())
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
