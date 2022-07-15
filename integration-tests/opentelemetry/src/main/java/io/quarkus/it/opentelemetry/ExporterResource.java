package io.quarkus.it.opentelemetry;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

@Path("/export")
public class ExporterResource {
    private static final Logger LOG = Logger.getLogger(ExporterResource.class);

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @GET
    @Path("/clear")
    public void clearExporter() {
        inMemorySpanExporter.reset();
    }

    @GET
    public List<SpanData> retrieve() {
        List<SpanData> toExport = inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export"))
                .collect(Collectors.toList());
        LOG.debug("toExport = " + toExport);
        return toExport;
    }
}
