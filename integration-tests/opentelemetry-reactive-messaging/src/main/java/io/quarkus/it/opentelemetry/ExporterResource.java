package io.quarkus.it.opentelemetry;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

@Path("/export")
public class ExporterResource {
    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @GET
    @Path("/clear")
    public void clearExporter() {
        inMemorySpanExporter.reset();
    }

    @GET
    public List<SpanData> retrieve() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export"))
                .collect(Collectors.toList());
    }
}
