package io.quarkus.oidc.token.propagation.reactive;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.security.credential.TokenCredential;

@Priority(Priorities.AUTHENTICATION)
public class AccessTokenRequestReactiveFilter implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AccessTokenRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = "Bearer ";

    @Inject
    Instance<TokenCredential> accessToken;

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (verifyTokenInstance(requestContext)) {
            propagateToken(requestContext);
        }
    }

    public void propagateToken(ResteasyReactiveClientRequestContext requestContext) {
        if (accessToken.get().getToken() != null) {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + accessToken.get().getToken());
        } else {
            LOG.debugf("Injected access token is null, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
        }
    }

    protected boolean verifyTokenInstance(ResteasyReactiveClientRequestContext requestContext) {
        if (!accessToken.isResolvable()) {
            LOG.debugf("Access token is not injected, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
            return false;
        }
        if (accessToken.isAmbiguous()) {
            LOG.debugf("More than one access token instance is available, aborting the request with HTTP 401 error");
            abortRequest(requestContext);
            return false;
        }

        return true;
    }

    protected void abortRequest(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.abortWith(Response.status(401).build());
    }
}
