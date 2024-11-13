package io.quarkus.opentelemetry.deployment.common.traces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.metrics.Meter;
import io.quarkus.opentelemetry.runtime.tracing.Traceless;

@Path("/class-level")
@Traceless
public class TracelessClassLevelResource {

    @Inject
    Meter meter;

    @GET
    public String classLevel() {
        meter.counterBuilder("class-level").build().add(1);
        return "class-level";
    }

    @GET
    @Path("/first-method")
    public String firstMethod() {
        meter.counterBuilder("first-method").build().add(1);
        return "first-method";
    }

    @Path("/second-method")
    @GET
    public String secondMethod() {
        meter.counterBuilder("second-method").build().add(1);
        return "second-method";
    }
}
