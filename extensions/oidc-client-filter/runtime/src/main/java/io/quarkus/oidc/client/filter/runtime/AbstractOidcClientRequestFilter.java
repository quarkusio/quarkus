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
    static final String REQUEST_FILTER_KEY = "io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter#this";
    private volatile boolean accessTokenNeedsRefresh;

    public AbstractOidcClientRequestFilter() {
        this.accessTokenNeedsRefresh = false;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (isClientFeatureDisabled()) {
            LOG.debug("OIDC client filter can not acquire and propagate tokens because "
                    + "OIDC client is disabled with `quarkus.oidc-client.enabled=false`");
            return;
        }
        if (refreshOnUnauthorized() && !accessTokenNeedsRefresh) {
            requestContext.setProperty(REQUEST_FILTER_KEY, this);
        }
        try {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + getAccessToken());
        } catch (DisabledOidcClientException ex) {
            LOG.debug("Client is disabled, acquiring and propagating the token is not necessary");
        } catch (Exception ex) {
            LOG.debugf("Access token is not available, cause: %s, aborting the request", ex.getMessage());
            throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    private String getAccessToken() {
        return awaitTokens().getAccessToken();
    }

    @Override
    protected boolean isForceNewTokens() {
        if (accessTokenNeedsRefresh()) {
            this.accessTokenNeedsRefresh = false;
            return true;
        }
        return super.isForceNewTokens();
    }

    /**
     * @return true if token that appears valid should be refreshed the next time this filter is applied
     */
    protected boolean refreshOnUnauthorized() {
        return false;
    }

    void refreshAccessToken() {
        this.accessTokenNeedsRefresh = true;
    }

    private boolean accessTokenNeedsRefresh() {
        return refreshOnUnauthorized() && accessTokenNeedsRefresh;
    }
}
