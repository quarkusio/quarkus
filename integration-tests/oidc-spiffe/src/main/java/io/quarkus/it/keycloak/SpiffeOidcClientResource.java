package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.client.OidcClient;

@Path("/spiffe/client")
public class SpiffeOidcClientResource {

    @Inject
    OidcClient oidcClient;

    @GET
    @Path("/token")
    public String getToken() {
        return oidcClient.getTokens().await().indefinitely().getAccessToken();
    }
}
