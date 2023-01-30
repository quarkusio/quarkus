package io.quarkus.resteasy.test.security;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
