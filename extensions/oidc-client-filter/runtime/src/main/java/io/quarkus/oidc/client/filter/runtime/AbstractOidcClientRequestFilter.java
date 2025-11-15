package io.quarkus.oidc.client.filter.runtime;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.logging.Logger;

import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.client.runtime.MethodDescription;
import io.quarkus.oidc.common.runtime.OidcConstants;

public abstract class AbstractOidcClientRequestFilter extends AbstractTokensProducer implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AbstractOidcClientRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";
    private static final String INVOKED_METHOD_PROP = "org.eclipse.microprofile.rest.client.invokedMethod";
    static final String REQUEST_FILTER_KEY = "io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter#this";
    private volatile boolean accessTokenNeedsRefresh;

    public AbstractOidcClientRequestFilter() {
        this.accessTokenNeedsRefresh = false;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (skipPropagation(requestContext)) {
            return;
        }

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

    private boolean skipPropagation(ClientRequestContext requestContext) {
        if (getMethodDescription() == null) {
            return false;
        }

        return !getMethodDescription().equals(getInvokedRestClientMethodSignature(requestContext));
    }

    private static MethodDescription getInvokedRestClientMethodSignature(ClientRequestContext requestContext) {
        if (requestContext.getProperty(INVOKED_METHOD_PROP) instanceof Method method) {
            return MethodDescription.ofMethod(method);
        }
        throw new IllegalStateException(INVOKED_METHOD_PROP + " property must not be null");
    }

    /**
     * This method is overridden by generated filter classes if the filter should only be applied on the REST client
     * method.
     *
     * @return REST client method description for which this filter should be applied; or null if applies to all methods
     */
    protected MethodDescription getMethodDescription() {
        return null;
    }
}
