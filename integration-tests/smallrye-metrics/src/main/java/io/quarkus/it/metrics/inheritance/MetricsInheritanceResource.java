package io.quarkus.it.metrics.inheritance;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;

@Path("/metricsinheritanceresource")
public class MetricsInheritanceResource {

    @Inject
    MetricRegistry metricRegistry;

    @Path("/registration")
    @GET
    @Produces("application/json")
    public List<String> getAllMetricNames() {
        return metricRegistry
                .getCounters((metricID, metric) -> metricID.getName().contains("Inheritance"))
                .keySet()
                .stream()
                .map(MetricID::getName)
                .collect(Collectors.toList());
    }

}
