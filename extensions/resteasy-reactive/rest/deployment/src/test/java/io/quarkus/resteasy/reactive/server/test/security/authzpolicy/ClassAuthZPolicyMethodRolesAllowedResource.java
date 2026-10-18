package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@AuthorizationPolicy(name = "permit-user")
@Path("authorization-policy-class-roles-allowed-method")
public class ClassAuthZPolicyMethodRolesAllowedResource {

    @RolesAllowed("admin")
    @GET
    public String principal(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @Path("no-roles-allowed")
    @GET
    public String noAuthorizationPolicy(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
