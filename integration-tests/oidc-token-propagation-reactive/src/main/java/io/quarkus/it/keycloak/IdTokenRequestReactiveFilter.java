package io.quarkus.it.keycloak;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.oidc.IdTokenCredential;

@Priority(Priorities.AUTHENTICATION)
public class IdTokenRequestReactiveFilter implements ResteasyReactiveClientRequestFilter {

    @Inject
    IdTokenCredential idToken;

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + idToken.getToken());
    }
}
