package io.quarkus.security.jpa;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.vertx.ext.web.RoutingContext;

/**
 * Test JAXRS endpoint with RolesAllowed specified at the class level
 */
@Path("/roles-class")
@RolesAllowed("user")
public class RolesEndpointClassLevel {

    @Inject
    RoutingContext routingContext;

    @GET
    public String echo(@Context SecurityContext sec) {
        return "Hello " + sec.getUserPrincipal().getName();
    }

    @Path("routing-context")
    @GET
    public boolean hasRoutingContext() {
        return routingContext != null;
    }

}
