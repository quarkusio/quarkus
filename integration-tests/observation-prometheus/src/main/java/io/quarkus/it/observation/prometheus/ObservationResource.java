package io.quarkus.it.observation.prometheus;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Path("/observation")
public class ObservationResource {

    @Inject
    ObservationRegistry registry;

    @Inject
    ObservedService service;

    @GET
    @Path("/manual")
    @Produces(MediaType.TEXT_PLAIN)
    public String manualObservation() {
        Observation observation = Observation.createNotStarted("manual.operation", registry);
        observation.lowCardinalityKeyValue("operation.type", "manual");
        return observation.observe(() -> "manual-result");
    }

    @GET
    @Path("/observed")
    @Produces(MediaType.TEXT_PLAIN)
    public String observed() {
        return service.doWork();
    }
}
