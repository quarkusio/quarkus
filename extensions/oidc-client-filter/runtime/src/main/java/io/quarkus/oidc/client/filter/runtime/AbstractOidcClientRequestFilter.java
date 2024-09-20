package io.quarkus.oidc.client.filter.runtime;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

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
        if (isClientFeatureDisabled()) {
            LOG.debug("OIDC client filter can not acquire and propagate tokens because "
                    + "OIDC client is disabled with `quarkus.oidc-client.enabled=false`");
            return;
        }
        try {
            final String accessToken = getAccessToken();
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + accessToken);
        } catch (DisabledOidcClientException ex) {
            LOG.debug("Client is disabled, acquiring and propagating the token is not necessary");
            return;
        } catch (Exception ex) {
            LOG.debugf("Access token is not available, cause: %s, aborting the request", ex.getMessage());
            throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    private String getAccessToken() {
        return awaitTokens().getAccessToken();
    }

}
