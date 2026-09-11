package io.quarkus.it.observation;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Path("/timer")
public class MetricsResource {

    @Inject
    MeterRegistry registry;

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimer(@PathParam("name") String name) {
        Timer timer = registry.find(name).timer();
        if (timer == null) {
            return Response.status(404).build();
        }
        return Response.ok(Map.of("name", name, "count", timer.count())).build();
    }
}
