package io.quarkus.it.vertx;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/protected")
public class CertificateRoleMappingResource {

    @Inject
    SecurityIdentity identity;

    @Authenticated
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/authenticated")
    public String name() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/authorized-user")
    @RolesAllowed("user")
    public String authorizedName() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/authorized-admin")
    @RolesAllowed("admin")
    public String authorizedAdmin() {
        return identity.getPrincipal().getName();
    }
}
