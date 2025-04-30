package io.quarkus.it.keycloak;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestReactiveFilter {

    @Inject
    OidcClientCreator oidcClientCreator;

    @Override
    protected Optional<OidcClient> client() {
        return Optional.of(oidcClientCreator.getOidcClient());
    }

}
