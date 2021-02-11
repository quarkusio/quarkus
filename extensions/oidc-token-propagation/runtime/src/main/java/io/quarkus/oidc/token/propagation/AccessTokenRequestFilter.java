package io.quarkus.oidc.token.propagation;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AccessTokenCredential;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class AccessTokenRequestFilter implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AccessTokenRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";

    @Inject
    AccessTokenCredential tokenCredential;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        try {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + tokenCredential.getToken());
        } catch (Exception ex) {
            LOG.debugf("Access token is not available, aborting the request with HTTP 401 error: %s", ex.getMessage());
            requestContext.abortWith(Response.status(401).build());
        }
    }
}
