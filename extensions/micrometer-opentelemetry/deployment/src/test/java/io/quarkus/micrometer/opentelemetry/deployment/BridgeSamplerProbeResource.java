package io.quarkus.micrometer.opentelemetry.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.micrometer.runtime.devui.DevUiMetricsSampler;

/** In-app probe (see the Task D4 rationale) — reports sampler bean resolvability over HTTP. */
@Path("/probe")
@ApplicationScoped
public class BridgeSamplerProbeResource {

    @GET
    @Path("/sampler-present")
    public boolean samplerPresent() {
        return CDI.current().select(DevUiMetricsSampler.class).isResolvable();
    }
}
