package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.RefreshToken;

@Path("/tenant-refresh")
public class TenantRefreshTokenResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @Inject
    RefreshToken refreshToken;

    @GET
    @Path("/tenant-web-app-refresh/api/user")
    @RolesAllowed("user")
    public String checkTokens() {
        return "userName: " + idToken.getName()
                + ", idToken: " + (idToken.getRawToken() != null)
                + ", accessToken: " + (accessToken.getRawToken() != null)
                + ", refreshToken: " + (refreshToken.getToken() != null);
    }

}
