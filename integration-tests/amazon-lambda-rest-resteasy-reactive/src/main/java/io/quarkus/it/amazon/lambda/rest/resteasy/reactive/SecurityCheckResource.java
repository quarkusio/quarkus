package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import jakarta.annotation.security.RolesAllowed;
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

    @RolesAllowed("user")
    @GET
    @Produces("text/plain")
    @Path("roles")
    public String checkRole(@Context SecurityContext ctx) {
        return Boolean.toString(ctx.isUserInRole("user"));
    }
}
