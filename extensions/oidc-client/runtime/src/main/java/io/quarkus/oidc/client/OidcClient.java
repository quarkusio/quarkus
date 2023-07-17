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
     * Get the grant access and refresh tokens.
     */
    default Uni<Tokens> getTokens() {
        return getTokens(Collections.emptyMap());
    }

    /**
     * Get the grant access and refresh tokens with additional grant parameters.
     *
     * @param additionalGrantParameters additional grant parameters
     * @return Uni<Tokens>
     */
    Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters);

    /**
     * Refresh and return a new pair of access and refresh tokens.
     * Note a refresh token grant will typically return not only a new access token but also a new refresh token.
     *
     * @param refreshToken refresh token
     * @return Uni<Tokens>
     */
    Uni<Tokens> refreshTokens(String refreshToken);

    /**
     * Revoke the access token.
     *
     * @param refreshToken access token which needs to be revoked
     * @return Uni<Boolean> true if the token has been revoked or found already being invalidated,
     *         false if the token can not be currently revoked in which case a revocation request might be retried.
     */
    Uni<Boolean> revokeAccessToken(String accessToken);
}
