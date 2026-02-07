package io.quarkus.oidc.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class DefaultOidcClientTokenResource {

    @Inject
    OidcClient client;

    @GET
    @Path("/client/token")
    public String clientToken() {
        return "Hello " + client.getTokens().map(Tokens::getAccessToken).await().indefinitely();
    }

}
