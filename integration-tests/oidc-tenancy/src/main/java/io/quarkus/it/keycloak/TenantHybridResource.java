package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;

@Path("/tenants")
public class TenantHybridResource {
    @Inject
    @IdToken
    JsonWebToken idToken;
    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("/{tenant-hybrid}/api/user")
    @RolesAllowed("user")
    public String userNameService() {
        return idToken.getName() != null ? (idToken.getName() + ":web-app") : (accessToken.getName() + ":service");
    }
}
