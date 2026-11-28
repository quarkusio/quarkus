package io.quarkus.it.observation.micrometer.opentelemetry;

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

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;

@Path("")
public class ExporterResource {

    @Inject
    InMemoryMetricExporter metricExporter;

    @GET
    @Path("/reset")
    public Response reset() {
        metricExporter.reset();
        return Response.ok().build();
    }

    @GET
    @Path("/export/metrics")
    public List<MetricData> exportMetrics(@QueryParam("name") String name) {
        return Collections.unmodifiableList(new ArrayList<>(
                metricExporter.getFinishedMetricItems().stream()
                        .filter(metricData -> name == null || metricData.getName().equals(name))
                        .collect(Collectors.toList())));
    }

    @ApplicationScoped
    static class InMemoryMetricExporterProducer {
        @Produces
        @Singleton
        InMemoryMetricExporter metricExporter() {
            return InMemoryMetricExporter.create();
        }
    }
}
