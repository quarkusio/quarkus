package io.quarkus.it.micrometer.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

@Path("/secured/{message}")
public class SecuredResourceOverlapping {

    @GET
    @Path("/details")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> details(@PathParam("message") String message) {
        return Uni.createFrom().item("details of " + message);
    }
}
