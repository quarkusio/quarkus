package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/code-flow-split-tokens")
@Authenticated
public class CodeFlowSplitTokensResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public String access() {
        AccessTokenCredential accessToken = identity.getCredential(AccessTokenCredential.class);
        RefreshToken refreshToken = identity.getCredential(RefreshToken.class);
        return identity.getPrincipal().getName()
                + ":" + (accessToken != null && accessToken.getToken() != null)
                + ":" + (refreshToken != null && refreshToken.getToken() != null);
    }
}
