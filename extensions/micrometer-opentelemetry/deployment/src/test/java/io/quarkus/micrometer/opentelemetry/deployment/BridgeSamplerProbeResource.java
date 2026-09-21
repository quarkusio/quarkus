package io.quarkus.micrometer.opentelemetry.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.micrometer.runtime.devui.DevUiMetricsSampler;

/**
 * In-app probe to understand when the Micrometer specific DevUiMetricsSampler is resolved.
 * If OTel metrics is present, sampling will be done there.
 * This class should only be resolved if only Micrometer metrics are being used.
 * We don't want to double-sample.
 */
@Path("/probe")
@ApplicationScoped
public class BridgeSamplerProbeResource {

    @GET
    @Path("/sampler-present")
    public boolean samplerPresent() {
        return CDI.current().select(DevUiMetricsSampler.class).isResolvable();
    }
}
