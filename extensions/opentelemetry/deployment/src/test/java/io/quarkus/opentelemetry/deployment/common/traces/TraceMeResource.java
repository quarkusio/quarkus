package io.quarkus.opentelemetry.deployment.common.traces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.metrics.Meter;

@Path("/trace-me")
public class TraceMeResource {

    @Inject
    Meter meter;

    @GET
    public String traceMe() {
        meter.counterBuilder("trace-me").build().add(1);
        return "trace-me";
    }
}
