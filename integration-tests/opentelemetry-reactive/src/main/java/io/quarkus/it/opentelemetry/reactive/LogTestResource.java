package io.quarkus.it.opentelemetry.reactive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/log-test")
public class LogTestResource {

    @GET
    @Path("/secured")
    @RolesAllowed("user")
    @Produces(MediaType.TEXT_PLAIN)
    public String secured() {
        return "secret";
    }

    @GET
    @Path("/public")
    @Produces(MediaType.TEXT_PLAIN)
    public String publicEndpoint() {
        return "hello";
    }
}
