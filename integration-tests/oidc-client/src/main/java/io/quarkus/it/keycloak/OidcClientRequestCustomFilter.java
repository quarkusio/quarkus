package io.quarkus.it.keycloak;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.Tokens;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter implements ClientRequestFilter {

    @Inject
    @NamedOidcClient("named")
    Tokens grantTokens;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + grantTokens.getAccessToken());
    }
}
