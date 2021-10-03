package io.quarkus.oidc.runtime;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheTokenStateManager implements TokenStateManager {
    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig, AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {
        return TokenStateManager.super.createTokenState(routingContext, oidcConfig, tokens, requestContext);
    }

    @Override
    @CacheResult(cacheName = "tokenState")
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, @CacheKey String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        return TokenStateManager.super.getTokens(routingContext, oidcConfig, tokenState, requestContext);
    }

    @Override
    @CacheInvalidate(cacheName = "tokenState")
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, @CacheKey String tokenState, OidcRequestContext<Void> requestContext) {
        return TokenStateManager.super.deleteTokens(routingContext, oidcConfig, tokenState, requestContext);
    }
}
