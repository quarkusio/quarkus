package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@AuthorizationPolicy(name = "viewer-augmenting-policy")
@Path("authz-policy-and-path-matching-policies")
public class AuthorizationPolicyAndPathMatchingPoliciesResource {

    @GET
    @Path("jax-rs-path-matching-http-perm")
    public boolean jaxRsPathMatchingHttpPerm(@Context SecurityContext securityContext) {
        return securityContext.isUserInRole("admin");
    }

    @GET
    @Path("path-matching-http-perm")
    public boolean pathMatchingHttpPerm(@Context SecurityContext securityContext) {
        return securityContext.isUserInRole("admin");
    }

    @RolesAllowed("admin")
    @GET
    @Path("roles-allowed-annotation")
    public String rolesAllowed(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
