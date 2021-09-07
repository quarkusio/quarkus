package io.quarkus.oidc;

import java.util.function.Supplier;

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
     *             {@link #createTokenState(RoutingContext, OidcTenantConfig, AuthorizationCodeTokens, CreateTokenStateRequestContext)}
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
            AuthorizationCodeTokens tokens, CreateTokenStateRequestContext requestContext) {
        return Uni.createFrom().item(createTokenState(routingContext, oidcConfig, tokens));
    }

    /**
     * Convert the token state into the authorization code flow tokens.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokens the token state
     *
     * @return the authorization code flow tokens
     *
     * @deprecated Use {@link #getTokens(RoutingContext, OidcTenantConfig, String, GetTokensRequestContext)} instead.
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
     * @param tokens the token state
     * @param requestContext the request context
     *
     * @return the authorization code flow tokens
     */
    default Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            String tokenState, GetTokensRequestContext requestContext) {
        return Uni.createFrom().item(getTokens(routingContext, oidcConfig, tokenState));
    }

    /**
     * Delete the token state.
     *
     * @param routingContext the request context
     * @param oidcConfig the tenant configuration
     * @param tokens the token state
     *
     * @deprecated Use {@link #deleteTokens(RoutingContext, OidcTenantConfig, String, DeleteTokensRequestContext)} instead
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
     * @param tokens the token state
     */
    default Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            DeleteTokensRequestContext requestContext) {
        deleteTokens(routingContext, oidcConfig, tokenState);
        return Uni.createFrom().voidItem();
    }

    /**
     * A context object that can be used to create a token state by running a blocking task.
     * <p>
     * Blocking providers should use this context to prevent excessive and unnecessary delegation to thread pools.
     */
    interface CreateTokenStateRequestContext {

        Uni<String> runBlocking(Supplier<String> function);
    }

    /**
     * A context object that can be used to convert the token state to the tokens by running a blocking task.
     * <p>
     * Blocking providers should use this context to prevent excessive and unnecessary delegation to thread pools.
     */
    interface GetTokensRequestContext {

        Uni<AuthorizationCodeTokens> runBlocking(Supplier<AuthorizationCodeTokens> function);
    }

    /**
     * A context object that can be used to delete the token state by running a blocking task.
     * <p>
     * Blocking providers should use this context to prevent excessive and unnecessary delegation to thread pools.
     */
    interface DeleteTokensRequestContext {

        Uni<Void> runBlocking(Supplier<Void> function);
    }
}
