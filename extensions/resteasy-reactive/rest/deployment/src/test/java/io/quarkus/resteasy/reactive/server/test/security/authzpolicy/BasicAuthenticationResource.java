package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.security.AuthorizationPolicy;

@Path("basic-auth-ann")
@BasicAuthentication
public class BasicAuthenticationResource {

    @GET
    public String noAuthorizationPolicy(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @Path("authorization-policy")
    @AuthorizationPolicy(name = "forbid-all-but-viewer")
    @GET
    public String authorizationPolicy(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
