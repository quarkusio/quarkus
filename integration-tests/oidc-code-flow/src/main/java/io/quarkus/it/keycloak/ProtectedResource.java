package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.runtime.RefreshToken;

@Path("/web-app")
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    RefreshToken refreshToken;

    @GET
    public String get() {
        return idToken.getClaim("preferred_username");
    }

    @GET
    @Path("refresh")
    public String refresh() {
        return refreshToken.getToken() != null ? "injected" : null;
    }
}
