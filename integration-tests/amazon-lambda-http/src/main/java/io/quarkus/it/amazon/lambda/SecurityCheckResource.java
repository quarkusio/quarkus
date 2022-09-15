package io.quarkus.it.amazon.lambda;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("security")
public class SecurityCheckResource {

    @GET
    @Produces("text/plain")
    @Path("username")
    public String getUsername(@Context SecurityContext ctx) {
        return ctx.getUserPrincipal().getName();
    }
}
