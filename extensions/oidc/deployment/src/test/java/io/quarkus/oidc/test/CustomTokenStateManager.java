package io.quarkus.oidc.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
@AlternativePriority(1)
public class CustomTokenStateManager implements TokenStateManager {

    @Inject
    DefaultTokenStateManager tokenStateManager;

    @Override
    public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens sessionContent, OidcRequestContext<String> requestContext) {
        return tokenStateManager.createTokenState(routingContext, oidcConfig, sessionContent, requestContext)
                .map(t -> (t + "|custom"));
    }

    @Override
    public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext) {
        if (!tokenState.endsWith("|custom")) {
            throw new IllegalStateException();
        }
        String defaultState = tokenState.substring(0, tokenState.length() - 7);
        return tokenStateManager.getTokens(routingContext, oidcConfig, defaultState, requestContext);
    }

    @Override
    public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
            OidcRequestContext<Void> requestContext) {
        if (!tokenState.endsWith("|custom")) {
            throw new IllegalStateException();
        }
        String defaultState = tokenState.substring(0, tokenState.length() - 7);
        return tokenStateManager.deleteTokens(routingContext, oidcConfig, defaultState, requestContext);
    }
}
