package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.PermissionsAllowed;

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

    @GET
    @Path("/{tenant-hybrid}/api/mp-scope")
    @PermissionsAllowed("microprofile-jwt")
    public String microProfileScopeService() {
        return accessToken.getClaim(OidcConstants.TOKEN_SCOPE);
    }

    @GET
    @Path("/{tenant-hybrid}/api/non-existent-scope")
    @PermissionsAllowed("microprofile-jwt")
    @PermissionsAllowed("nonexistent-scope")
    public String nonExistentScopeService() {
        return accessToken.getClaim(OidcConstants.TOKEN_SCOPE);
    }
}
