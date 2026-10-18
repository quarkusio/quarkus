package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;

@Path("/github-no-id-token-no-user-info")
public class CodeFlowNoIdTokenNoUserInfo {

    @Inject
    AccessTokenCredential accessTokenCredential;

    @GET
    @Authenticated
    public String getAccessTokenCredential() {
        return accessTokenCredential.getToken();
    }

}
