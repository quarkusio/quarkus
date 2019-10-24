package io.quarkus.it.keycloak;

import java.util.List;

import javax.inject.Inject;
import javax.security.auth.AuthPermission;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/permission")
public class ProtectedResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Permission> permissions() {
        if (identity.checkPermissionBlocking(new AuthPermission("Permission Resource"))) {
            return identity.getAttribute("permissions");
        }
        throw new ForbiddenException();
    }

    @Path("/claim-protected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Permission> claimProtected() {
        return identity.getAttribute("permissions");
    }

    @Path("/http-response-claim-protected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Permission> httpResponseClaimProtected() {
        return identity.getAttribute("permissions");
    }
}
