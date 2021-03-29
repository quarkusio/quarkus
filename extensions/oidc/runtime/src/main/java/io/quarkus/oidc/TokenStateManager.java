package io.quarkus.oidc;

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

    String createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig, AuthorizationCodeTokens tokens);

    AuthorizationCodeTokens getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState);

    void deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState);
}
