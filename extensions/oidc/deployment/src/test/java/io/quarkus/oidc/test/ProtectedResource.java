package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @Inject
    OidcConfig config;

    @GET
    public String getName() {
        return idToken.getName();
    }

    @GET
    @Path("tenant/{id}")
    public String getTenantName(@PathParam("id") String tenantId) {
        return tenantId + ":" + idToken.getName();
    }

    @GET
    @Path("logout")
    public void logout() {
        throw new RuntimeException("Logout must be handled by CodeAuthenticationMechanism");
    }

    @Path("access-token-name")
    @GET
    public String accessTokenName() {
        if (!OidcConfig.getDefaultTenant(config).authentication().verifyAccessToken()) {
            throw new IllegalStateException("Access token verification should be enabled");
        }
        return accessToken.getName();
    }
}
