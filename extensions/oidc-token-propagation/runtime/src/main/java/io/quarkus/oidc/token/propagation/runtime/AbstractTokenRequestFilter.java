package io.quarkus.oidc.token.propagation.runtime;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public abstract class AbstractTokenRequestFilter implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AbstractTokenRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";

    public void propagateToken(ClientRequestContext requestContext, String token) throws IOException {
        if (token != null) {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + token);
        } else {
            LOG.debugf("Injected access token is null, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
        }
    }

    protected boolean verifyTokenInstance(ClientRequestContext requestContext, Instance<?> instance) throws IOException {
        if (!instance.isResolvable()) {
            LOG.debugf("Access token is not injected, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
            return false;
        }
        if (instance.isAmbiguous()) {
            LOG.debugf("More than one access token instance is available, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
            return false;
        }

        return true;
    }

    protected void abortRequest(ClientRequestContext requestContext) {
        requestContext.abortWith(Response.status(401).build());
    }
}
