package io.quarkus.oidc.token.propagation;

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
