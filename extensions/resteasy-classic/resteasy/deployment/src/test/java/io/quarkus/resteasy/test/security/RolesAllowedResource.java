package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.identity.CurrentIdentityAssociation;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/roles")
@PermitAll
public class RolesAllowedResource {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @GET
    @RolesAllowed({ "user", "admin" })
    public String defaultSecurity() {
        return "default";
    }

    @Path("/admin")
    @RolesAllowed("admin")
    @GET
    public String admin() {
        return "admin";
    }

    @Path("/admin/security-identity")
    @RolesAllowed("admin")
    @GET
    public String getSecurityIdentity() {
        return currentIdentityAssociation.getIdentity().getPrincipal().getName();
    }
}
