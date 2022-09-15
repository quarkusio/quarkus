package io.quarkus.oidc.client.filter;

import java.io.IOException;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.filter.runtime.OidcClientFilterConfig;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.common.runtime.OidcConstants;

@Provider
@Singleton
@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestFilter extends AbstractTokensProducer implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(OidcClientRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";
    @Inject
    OidcClientFilterConfig oidcClientFilterConfig;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        try {
            final String accessToken = getAccessToken();
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + accessToken);
        } catch (DisabledOidcClientException ex) {
            requestContext.abortWith(Response.status(500).build());
        } catch (Exception ex) {
            LOG.debugf("Access token is not available, aborting the request with HTTP 401 error: %s", ex.getMessage());
            requestContext.abortWith(Response.status(401).build());
        }
    }

    private String getAccessToken() {
        // It should be reactive when run with Resteasy Reactive
        return awaitTokens().getAccessToken();
    }

    protected Optional<String> clientId() {
        return oidcClientFilterConfig.clientName;
    }
}
