package io.quarkus.micrometer.deployment.devui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.micrometer.core.instrument.MeterRegistry;

@Path("/metrics-test")
@ApplicationScoped
public class MetricsResource {

    private final MeterRegistry registry;

    public MetricsResource(MeterRegistry registry) {
        this.registry = registry;
    }

    @GET
    @Path("/hit")
    public String hit() {
        registry.counter("demo.hits", "endpoint", "hit").increment();
        registry.gauge("demo.gauge", 42.0);
        return "ok";
    }
}
