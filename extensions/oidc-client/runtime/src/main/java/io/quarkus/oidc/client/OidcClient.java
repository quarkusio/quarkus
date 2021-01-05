package io.quarkus.oidc.client;

import java.io.Closeable;

import io.smallrye.mutiny.Uni;

/**
 * Token grant client
 */
public interface OidcClient extends Closeable {

    /**
     * Returns the grant tokens
     */
    Uni<Tokens> getTokens();

    /**
     * Refreshes the grant tokens
     */
    Uni<Tokens> refreshTokens(String refreshToken);
}
