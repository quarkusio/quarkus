package io.quarkus.oidc.client.filter;

import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.client.filter.runtime.OidcClientFilterConfig;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestFilter extends AbstractOidcClientRequestFilter {

    @Inject
    OidcClientFilterConfig oidcClientFilterConfig;

    protected Optional<String> clientId() {
        return oidcClientFilterConfig.clientName;
    }
}
