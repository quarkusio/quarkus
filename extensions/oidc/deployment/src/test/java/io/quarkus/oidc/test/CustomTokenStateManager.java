package io.quarkus.oidc.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
@AlternativePriority(1)
public class CustomTokenStateManager implements TokenStateManager {

    @Inject
    DefaultTokenStateManager tokenStateManager;

    @Override
    public String createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            AuthorizationCodeTokens sessionContent) {
        return tokenStateManager.createTokenState(routingContext, oidcConfig, sessionContent) + "|custom";
    }

    @Override
    public AuthorizationCodeTokens getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
            String tokenState) {
        if (!tokenState.endsWith("|custom")) {
            throw new IllegalStateException();
        }
        String defaultState = tokenState.substring(0, tokenState.length() - 7);
        return tokenStateManager.getTokens(routingContext, oidcConfig, defaultState);
    }

    @Override
    public void deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState) {
        if (!tokenState.endsWith("|custom")) {
            throw new IllegalStateException();
        }
    }
}
