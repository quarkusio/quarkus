package io.quarkus.oidc.client;

import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.mutiny.Uni;

@Path("/clients")
public class OidcClientsResource {

    @Inject
    OidcClients clients;

    @GET
    @Path("token/{id}")
    public Uni<String> tokenUni(@PathParam("id") String oidcClientId) {
        return getClient(oidcClientId).getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("tokens/{id}")
    public Uni<String> grantTokensUni(@PathParam("id") String oidcClientId) {
        return getClient(oidcClientId).getTokens().flatMap(tokens -> createTokensString(tokens));
    }

    @GET
    @Path("tokenOnDemand")
    public Uni<String> tokenOnDemand() {
        OidcClientConfig cfg = new OidcClientConfig();
        cfg.setId("dynamic");
        cfg.setAuthServerUrl(ConfigProvider.getConfig().getValue("quarkus.oidc-client.auth-server-url", String.class));
        cfg.setClientId(ConfigProvider.getConfig().getValue("quarkus.oidc-client.client-id", String.class));
        cfg.getCredentials().setSecret("secret");
        return clients.newClient(cfg).onItem().transformToUni(new Function<OidcClient, Uni<? extends String>>() {

            @Override
            public Uni<String> apply(OidcClient client) {
                return client.getTokens().flatMap(tokens -> createTokensString(tokens));
            }
        });
    }

    private Uni<String> createTokensString(Tokens tokens) {
        return tokensAreInitialized(tokens) ? Uni.createFrom().item(tokens.getAccessToken() + " " + tokens.getRefreshToken())
                : Uni.createFrom().failure(new InternalServerErrorException());
    }

    private boolean tokensAreInitialized(Tokens tokens) {
        return tokens.getAccessToken() != null && tokens.getAccessTokenExpiresAt() != null;
    }

    private OidcClient getClient(String id) {
        return "default".equals(id) ? clients.getClient() : clients.getClient(id);
    }
}
