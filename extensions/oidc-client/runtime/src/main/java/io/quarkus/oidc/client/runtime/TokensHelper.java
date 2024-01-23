package io.quarkus.oidc.client.runtime;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

public class TokensHelper {

    @SuppressWarnings("unused")
    private volatile TokenRequestState tokenRequestState;

    private static final AtomicReferenceFieldUpdater<TokensHelper, TokenRequestState> tokenRequestStateUpdater = AtomicReferenceFieldUpdater
            .newUpdater(TokensHelper.class, TokenRequestState.class, "tokenRequestState");

    public void initTokens(OidcClient oidcClient) {
        //init the tokens, this just happens in a blocking manner for now
        tokenRequestStateUpdater.set(this, new TokenRequestState(oidcClient.getTokens().await().indefinitely()));
    }

    public Uni<Tokens> getTokens(OidcClient oidcClient) {
        return getTokens(oidcClient, false);
    }

    public Uni<Tokens> getTokens(OidcClient oidcClient, boolean forceNewTokens) {
        TokenRequestState currentState = null;
        TokenRequestState newState = null;
        //if the tokens are expired we refresh them in an async manner
        //we use CAS to make sure we only make a single request
        for (;;) {
            currentState = tokenRequestStateUpdater.get(this);
            if (currentState == null) {
                //init the initial state
                //note that this can still happen at runtime as if there is an error then the state will be null
                newState = new TokenRequestState(prepareUni(oidcClient.getTokens()));
                if (tokenRequestStateUpdater.compareAndSet(this, currentState, newState)) {
                    return newState.tokenUni;
                }
                //rerun the CAS loop
            } else if (currentState.tokenUni != null) {
                return currentState.tokenUni;
            } else {
                Tokens tokens = currentState.tokens;
                if (forceNewTokens || tokens.isAccessTokenExpired() || tokens.isAccessTokenWithinRefreshInterval()) {
                    newState = new TokenRequestState(
                            prepareUni((!forceNewTokens && tokens.getRefreshToken() != null && !tokens.isRefreshTokenExpired())
                                    ? oidcClient.refreshTokens(tokens.getRefreshToken())
                                    : oidcClient.getTokens()));
                    if (tokenRequestStateUpdater.compareAndSet(this, currentState, newState)) {
                        return newState.tokenUni;
                    }
                    //rerun the CAS loop
                } else {
                    return Uni.createFrom().item(tokens);
                }
            }
        }
    }

    private Uni<Tokens> prepareUni(Uni<Tokens> tokens) {
        return tokens.onItemOrFailure().invoke(new BiConsumer<Tokens, Throwable>() {
            @Override
            public void accept(Tokens tokens, Throwable throwable) {
                //we only have a single outstanding request
                //so we don't need to CAS
                if (tokens != null) {
                    tokenRequestStateUpdater.set(TokensHelper.this, new TokenRequestState(tokens));
                } else {
                    tokenRequestStateUpdater.set(TokensHelper.this, null);
                }
            }
        });
    }

    static class TokenRequestState {
        final Tokens tokens;
        final Uni<Tokens> tokenUni;

        TokenRequestState(Tokens tokens) {
            this.tokens = tokens;
            this.tokenUni = null;
        }

        TokenRequestState(Uni<Tokens> tokensUni) {
            this.tokens = null;
            this.tokenUni = tokensUni;
        }
    }
}
