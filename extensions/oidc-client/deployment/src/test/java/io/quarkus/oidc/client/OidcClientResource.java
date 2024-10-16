package io.quarkus.oidc.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.oidc.client.spi.TokenProvider;
import io.smallrye.mutiny.Uni;

@Path("/client")
public class OidcClientResource {

    @Inject
    OidcClient client;

    @Inject
    TokenProvider tokenProvider;

    @Inject
    @NamedOidcClient("key")
    OidcClient keyClient;

    @GET
    @Path("token-key")
    public Uni<String> tokenFromPrivateKeyUni() {
        return keyClient.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("token")
    public Uni<String> tokenUni() {
        return client.getTokens().flatMap(tokens -> Uni.createFrom().item(tokens.getAccessToken()));
    }

    @GET
    @Path("tokenprovider")
    public Uni<String> tokenProvider() {
        return tokenProvider.getAccessToken();
    }

    @GET
    @Path("tokens")
    public Uni<String> grantTokensUni() {
        return client.getTokens().flatMap(this::createTokensString);
    }

    @GET
    @Path("refresh-tokens")
    public Uni<String> refreshGrantTokens(@QueryParam("refreshToken") String refreshToken) {
        return client.refreshTokens(refreshToken).flatMap(this::createTokensString);
    }

    private Uni<String> createTokensString(Tokens tokens) {
        return tokensAreInitialized(tokens) ? Uni.createFrom().item(tokens.getAccessToken() + " " + tokens.getRefreshToken())
                : Uni.createFrom().failure(new InternalServerErrorException());
    }

    private boolean tokensAreInitialized(Tokens tokens) {
        return tokens.getAccessToken() != null && tokens.getAccessTokenExpiresAt() != null && tokens.getRefreshToken() != null;
    }
}
