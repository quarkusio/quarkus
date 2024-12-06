package io.quarkus.oidc;

import io.smallrye.mutiny.Uni;

/**
 * Provides access to OIDC UserInfo, token introspection and revocation endpoints.
 */
public interface OidcProviderClient {

    /**
     * Get UserInfo.
     *
     * @param accessToken access token which is required to access a UserInfo endpoint.
     * @return Uni<UserInfo> {@link UserInfo}
     */
    Uni<UserInfo> getUserInfo(String accessToken);

    /**
     * Introspect the access token.
     *
     * @param accessToken access oken which must be introspected.
     * @return Uni<TokenIntrospection> {@link TokenIntrospection}
     */
    Uni<TokenIntrospection> introspectAccessToken(String accessToken);

    /**
     * Revoke the access token.
     *
     * @param accessToken access token which needs to be revoked.
     * @return Uni<Boolean> true if the access token has been revoked or found already being invalidated,
     *         false if the access token can not be currently revoked in which case a revocation request might be retried.
     */
    Uni<Boolean> revokeAccessToken(String accessToken);

    /**
     * Revoke the refresh token.
     *
     * @param refreshToken refresh token which needs to be revoked.
     * @return Uni<Boolean> true if the refresh token has been revoked or found already being invalidated,
     *         false if the refresh token can not be currently revoked in which case a revocation request might be retried.
     */
    Uni<Boolean> revokeRefreshToken(String refreshToken);

}
