package io.quarkus.oidc.token.propagation.deployment.test;

import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;

public class CustomAccessTokenRequestFilter extends AccessTokenRequestFilter {

    @Override
    protected String getClientName() {
        return "exchange";
    }

    @Override
    protected boolean isExchangeToken() {
        return true;
    }

}
