package io.quarkus.oidc.client;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.Uni;

@Path("/")
public class NamedOidcClientResource {

    @Inject
    @NamedOidcClient("client1")
    OidcClient client1;

    @Inject
    @NamedOidcClient("client2")
    OidcClient client2;

    @Inject
    @NamedOidcClient("client1")
    Tokens tokens1;

    @Inject
    @NamedOidcClient("client2")
    Tokens tokens2;

    @Inject
    @Any
    Instance<OidcClient> clients;

    @GET
    @Path("/client1/token")
    public Uni<String> client1TokenUni() {
        return client1.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("/client2/token")
    public Uni<String> client2TokenUni() {
        return client2.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("/client1/token/singleton")
    public String accessToken1() {
        return tokens1.getAccessToken();
    }

    @GET
    @Path("/client2/token/singleton")
    public String accessToken2() {
        return tokens2.getAccessToken();
    }

    @GET
    @Path("/{client}/token/programmatic")
    public Uni<String> programmaticTokenUni(@PathParam("client") String client) {
        OidcClient oidcClient = clients.select(NamedOidcClient.Literal.of(client)).get();
        return oidcClient.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("/{client}/token/programmatic-arc")
    public Uni<String> programmaticTokenUniArc(@PathParam("client") String client) {
        OidcClient oidcClient = Arc.container().instance(OidcClient.class, NamedOidcClient.Literal.of(client)).get();
        return oidcClient.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }
}
