package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomJwtBearerForceNewTokenFilter extends AbstractOidcClientRequestFilter {

    @Override
    protected Map<String, String> additionalParameters() {
        return Map.of(OidcConstants.CLIENT_ASSERTION, "123456");
    }

    @Override
    protected boolean isForceNewTokens() {
        // Easiest way to force requesting new tokens, instead of
        // manipulating the token expiration time
        return true;
    }

    @Override
    protected Optional<String> clientId() {
        return Optional.of("jwtbearer-forcenewtoken");
    }
}
