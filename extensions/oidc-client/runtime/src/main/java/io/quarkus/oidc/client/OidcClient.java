package io.quarkus.oidc.client;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * Token grant client
 */
public interface OidcClient extends Closeable {

    /**
     * Returns the grant tokens
     */
    default Uni<Tokens> getTokens() {
        return getTokens(Collections.emptyMap());
    }

    /**
     * Returns the grant tokens
     *
     * @param additionalGrantParameters additional grant parameters
     */
    Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters);

    /**
     * Refreshes the grant tokens
     */
    Uni<Tokens> refreshTokens(String refreshToken);
}
