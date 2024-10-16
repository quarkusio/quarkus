package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/roles-blocking")
@PermitAll
@Blocking
public class RolesAllowedBlockingResource {

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

    @NonBlocking
    @Path("/admin/security-identity")
    @RolesAllowed("admin")
    @GET
    public String getSecurityIdentity() {
        return currentIdentityAssociation.getIdentity().getPrincipal().getName();
    }

}
