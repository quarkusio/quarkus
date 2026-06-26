package io.quarkus.it.opentelemetry.observation;

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
    @Path("/nested")
    @Produces(MediaType.TEXT_PLAIN)
    public String nestedObservation() {
        Observation parent = Observation.start("parent.operation", registry);
        try (Observation.Scope parentScope = parent.openScope()) {
            Observation child = Observation.start("child.operation", registry);
            try (Observation.Scope childScope = child.openScope()) {
                // nested work
            }
            child.stop();
        }
        parent.stop();
        return "nested-result";
    }

    @GET
    @Path("/observed")
    @Produces(MediaType.TEXT_PLAIN)
    public String observed() {
        return service.doWork();
    }

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_PLAIN)
    public String error() {
        Observation observation = Observation.start("error.operation", registry);
        try (Observation.Scope scope = observation.openScope()) {
            throw new RuntimeException("test error");
        } catch (Exception e) {
            observation.error(e);
            observation.stop();
            return "error-handled";
        }
    }
}
