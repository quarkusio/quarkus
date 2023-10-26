package io.quarkus.it.keycloak;

import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;

public class CustomAccessTokenRequestFilter extends AccessTokenRequestFilter {
    @Override
    protected String getClientName() {
        return "exchange-token";
    }

    @Override
    protected boolean isExchangeToken() {
        return true;
    }
}
