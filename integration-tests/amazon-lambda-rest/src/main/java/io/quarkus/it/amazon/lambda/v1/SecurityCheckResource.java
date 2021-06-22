package io.quarkus.it.amazon.lambda.v1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@Path("security")
public class SecurityCheckResource {

    @GET
    @Produces("text/plain")
    @Path("username")
    public String getUsername(@Context SecurityContext ctx) {
        return ctx.getUserPrincipal().getName();
    }
}
