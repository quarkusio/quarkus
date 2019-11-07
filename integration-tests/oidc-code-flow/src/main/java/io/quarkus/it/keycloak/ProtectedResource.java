package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/web-app")
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @Inject
    SecurityIdentity identity;

    @GET
    public String getName() {
        return idToken.getName();
    }

    @GET
    @Path("access")
    public String getAccessToken() {
        return accessToken.getRawToken() != null && !accessToken.getRawToken().isEmpty() ? "AT injected" : "";
        // or get it with identity.getCredential(AccessTokenCredential.class).getToken();
    }

    @GET
    @Path("refresh")
    public String refresh() {
        String refreshToken = identity.getCredential(AccessTokenCredential.class).getRefreshToken();
        return refreshToken != null && !refreshToken.isEmpty() ? "RT injected" : "";
    }
}
