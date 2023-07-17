package io.quarkus.oidc.client;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter implements ClientRequestFilter {

    @Inject
    Tokens grantTokens;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + grantTokens.getAccessToken());
    }
}
