package io.quarkus.it.openapi.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

@Path("/security")
public class TestSecurityResource {

    @RolesAllowed("admin")
    @GET
    @Path("reactive-routes")
    public String reactiveRoutes(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @RouteFilter(401)
    public void doNothing(RoutingContext routingContext) {
        // here so that the Reactive Routes extension activates CDI request context
        routingContext.response().putHeader("reactive-routes-filter", "true");
        routingContext.next();
    }

}
