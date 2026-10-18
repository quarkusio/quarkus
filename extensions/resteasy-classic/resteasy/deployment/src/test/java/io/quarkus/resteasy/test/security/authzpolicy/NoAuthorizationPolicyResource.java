package io.quarkus.resteasy.test.security.authzpolicy;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.identity.SecurityIdentity;

@Path("no-authorization-policy")
public class NoAuthorizationPolicyResource {

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("unsecured")
    public String unsecured() {
        return securityIdentity.getPrincipal().getName();
    }

    @GET
    @Path("jax-rs-path-matching-http-perm")
    public String jaxRsPathMatchingHttpPerm(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @GET
    @Path("path-matching-http-perm")
    public String pathMatchingHttpPerm(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @RolesAllowed("admin")
    @GET
    @Path("roles-allowed-annotation")
    public String rolesAllowed(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
