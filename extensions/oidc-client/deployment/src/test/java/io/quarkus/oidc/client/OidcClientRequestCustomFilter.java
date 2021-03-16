package io.quarkus.oidc.client;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter implements ClientRequestFilter {

    @Inject
    Tokens grantTokens;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + grantTokens.getAccessToken());
    }
}
