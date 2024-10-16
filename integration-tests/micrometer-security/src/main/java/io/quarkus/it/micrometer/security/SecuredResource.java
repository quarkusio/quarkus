package io.quarkus.it.micrometer.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

@Path("/secured")
public class SecuredResource {

    @GET
    @Path("/{message}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> message(@PathParam("message") String message) {
        return Uni.createFrom().item(message);
    }
}
