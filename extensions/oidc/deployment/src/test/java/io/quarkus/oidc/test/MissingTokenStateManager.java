package io.quarkus.oidc.test;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Token state manager which simulates an external storage that has lost the token state,
 * by returning a null value from {@link #getTokens} instead of failing with an exception.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class MissingTokenStateManager implements TokenStateManager {

    private final AtomicBoolean tokenStateIsMissing = new AtomicBoolean();

    @Inject
    DefaultTokenStateManager tokenStateManager;

    void loseTokenState() {
        tokenStateIsMissing.set(true);
    }

    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext) {
        return tokenStateManager.createTokenState(routingContext, oidcConfig, tokens, requestContext);
    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        if (tokenStateIsMissing.get()) {
            return Uni.createFrom().nullItem();
        }
        return tokenStateManager.getTokens(routingContext, oidcConfig, tokenState, requestContext);
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        return tokenStateManager.deleteTokens(routingContext, oidcConfig, tokenState, requestContext);
    }
}
