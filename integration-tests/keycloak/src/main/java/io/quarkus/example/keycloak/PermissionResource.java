package io.quarkus.example.keycloak;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.idm.authorization.Permission;

@Path("/api/permission")
public class PermissionResource {

    @Inject
    KeycloakSecurityContext keycloakSecurityContext;

    @GET
    @Path("/{name}")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Permission> permissions() {
        return keycloakSecurityContext.getAuthorizationContext().getPermissions();
    }
}
