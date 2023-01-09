package io.quarkus.oidc.client.filter.runtime;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.common.runtime.OidcConstants;

public class AbstractOidcClientRequestFilter extends AbstractTokensProducer implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AbstractOidcClientRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";

    public AbstractOidcClientRequestFilter() {
    }

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
        return awaitTokens().getAccessToken();
    }

}
