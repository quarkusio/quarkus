package io.quarkus.oidc.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;

@Path("/public-client")
public class OidcPublicClientResource {

    @Inject
    @NamedOidcClient("public")
    OidcClient client;

    @GET
    @Path("token")
    public Uni<String> tokenUni() {
        return client.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }
}
