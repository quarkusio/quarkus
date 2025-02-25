package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResourceWithJwtAccessToken {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @Inject
    OidcConfig config;

    @GET
    public String getName() {
        return idToken.getName() + ":" + OidcConfig.getDefaultTenant(config).authentication().verifyAccessToken();
    }
}
