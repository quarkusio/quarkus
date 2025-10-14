package io.quarkus.oidc.client.filter;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.client.filter.runtime.OidcClientFilterConfig;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestFilter extends AbstractOidcClientRequestFilter {

    @Inject
    public OidcClientFilterConfig oidcClientFilterConfig;

    protected Optional<String> clientId() {
        return oidcClientFilterConfig.clientName();
    }

    @Override
    protected boolean refreshOnUnauthorized() {
        return oidcClientFilterConfig.refreshOnUnauthorized();
    }
}
