package io.quarkus.oidc;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Authorization Code Flow Token State Manager.
 * It converts the ID, access and refresh tokens returned in the authorization code grant response into a token state
 * for OIDC Code AuthenticationMechanism to keep it as a session cookie.
 * 
 * For example, default TokenStateManager concatenates all 3 tokens into a single String but does not persist it.
 * Custom TokenStateManager may choose to keep the tokens in the external storage (DB, file system, etc) and return
 * a reference to this storage.
 */
public interface TokenStateManager {

    /**
     * Convert the authorization code flow tokens into a token state.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokens the authorization code flow tokens
     *
     * @return the token state
     *
     * @deprecated Use
     *             {@link #createTokenState(RoutingContext, OidcTenantConfig, AuthorizationCodeTokens, OidcRequestContext)}
     *
     */
    @Deprecated
    default String createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens) {
        throw new UnsupportedOperationException("createTokenState is not implemented");
    }

    /**
     * Convert the authorization code flow tokens into a token state.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokens the authorization code flow tokens
     * @param requestContext the request context
     *
     * @return the token state
     */
    default Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {
        return Uni.createFrom().item(createTokenState(routingContext, oidcConfig, tokens));
    }

    /**
     * Convert the token state into the authorization code flow tokens.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokenState the token state
     *
     * @return the authorization code flow tokens
     *
     * @deprecated Use {@link #getTokens(RoutingContext, OidcTenantConfig, String, OidcRequestContext)} instead.
     */
    @Deprecated
    default AuthorizationCodeTokens getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState) {
        throw new UnsupportedOperationException("getTokens is not implemented");
    }

    /**
     * Convert the token state into the authorization code flow tokens.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokenState the token state
     * @param requestContext the request context
     *
     * @return the authorization code flow tokens
     */
    default Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        return Uni.createFrom().item(getTokens(routingContext, oidcConfig, tokenState));
    }

    /**
     * Delete the token state.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokenState the token state
     *
     * @deprecated Use {@link #deleteTokens(RoutingContext, OidcTenantConfig, String, OidcRequestContext)} instead
     */
    @Deprecated
    default void deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState) {
        throw new UnsupportedOperationException("deleteTokens is not implemented");
    }

    /**
     * Delete the token state.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokenState the token state
     */
    default Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        deleteTokens(routingContext, oidcConfig, tokenState);
        return Uni.createFrom().voidItem();
    }

}
