package io.quarkus.oidc.client.reactive.filter;

import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;
import io.quarkus.oidc.client.reactive.filter.runtime.OidcClientReactiveFilterConfig;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestReactiveFilter extends AbstractOidcClientRequestReactiveFilter {

    @Inject
    OidcClientReactiveFilterConfig config;

    @Override
    protected Optional<String> clientId() {
        return config.clientName;
    }
}
