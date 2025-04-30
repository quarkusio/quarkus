package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;

@Path("/code-flow-opaque-access-token")
@Authenticated
public class CodeFlowOpaqueAccessTokenResource {

    @Inject
    JsonWebToken jwtAccessToken;

    @Inject
    AccessTokenCredential accessTokenCredential;

    @GET
    public String getAccessTokenCredential() {
        return accessTokenCredential.getToken();
    }

    @GET
    @Path("/jwt-access-token")
    public String getJwtAccessToken() {
        return jwtAccessToken.getRawToken();
    }

}
