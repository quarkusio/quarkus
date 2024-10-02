package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@RolesAllowed("admin")
@Path("roles-allowed-class-authorization-policy-method")
public class ClassRolesAllowedMethodAuthZPolicyResource {

    @AuthorizationPolicy(name = "permit-user")
    @GET
    public String principal(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @Path("no-authz-policy")
    @GET
    public String noAuthorizationPolicy(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
