package io.quarkus.it.metrics.inheritance;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
