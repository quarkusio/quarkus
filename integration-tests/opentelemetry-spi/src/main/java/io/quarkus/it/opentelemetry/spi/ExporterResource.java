package io.quarkus.it.opentelemetry.spi;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapPropagator;

@Path("")
public class ExporterResource {
    @Inject
    OpenTelemetry openTelemetry;

    @GET
    @Path("/export/propagation")
    public List<String> exportPropagation() throws NoSuchFieldException, IllegalAccessException {
        TextMapPropagator textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        return new ArrayList<>(textMapPropagator.fields());
    }
}
