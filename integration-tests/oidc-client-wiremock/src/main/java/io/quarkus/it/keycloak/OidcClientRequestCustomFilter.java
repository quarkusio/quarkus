package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestFilter {

    @Override
    protected Map<String, String> additionalParameters() {
        return Map.of(OidcConstants.CLIENT_ASSERTION, "123456");
    }

    @Override
    protected Optional<String> clientId() {
        return Optional.of("jwtbearer");
    }
}
