package io.quarkus.oidc.client.reactive.filter.runtime;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.client.runtime.MethodDescription;
import io.quarkus.oidc.common.runtime.OidcConstants;

public abstract class AbstractOidcClientRequestReactiveFilter extends AbstractTokensProducer
        implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AbstractOidcClientRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";
    static final String REQUEST_FILTER_KEY = "io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter#this";
    private volatile boolean accessTokenNeedsRefresh = false;

    protected void initTokens() {
        if (earlyTokenAcquisition) {
            LOG.debug("Token acquisition will be delayed until this filter is executed to avoid blocking an IO thread");
        }
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (skipPropagation(requestContext)) {
            return;
        }

        if (isClientFeatureDisabled()) {
            LOG.debug("OIDC client filter can not acquire and propagate tokens because "
                    + "OIDC client is disabled with `quarkus.oidc-client.enabled=false`");
            return;
        }
        requestContext.suspend();

        if (refreshOnUnauthorized() && !accessTokenNeedsRefresh) {
            requestContext.setProperty(REQUEST_FILTER_KEY, this);
        }

        getTokens().subscribe().with(new Consumer<>() {
            @Override
            public void accept(Tokens tokens) {
                requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                        BEARER_SCHEME_WITH_SPACE + tokens.getAccessToken());
                requestContext.resume();
            }
        }, new Consumer<>() {
            @Override
            public void accept(Throwable t) {
                if (t instanceof DisabledOidcClientException) {
                    LOG.debug("Client is disabled, acquiring and propagating the token is not necessary");
                    requestContext.resume();
                } else {
                    LOG.debugf("Access token is not available, cause: %s, aborting the request", t.getMessage());
                    requestContext.resume((t instanceof RuntimeException) ? t : new RuntimeException(t));
                }
            }
        });
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

    private boolean skipPropagation(ResteasyReactiveClientRequestContext requestContext) {
        if (getMethodDescription() == null) {
            return false;
        }

        return !getMethodDescription().equals(getInvokedRestClientMethodSignature(requestContext));
    }

    private static MethodDescription getInvokedRestClientMethodSignature(ResteasyReactiveClientRequestContext requestContext) {
        if (requestContext.getProperty(RestClientRequestContext.INVOKED_METHOD_PROP) instanceof Method method) {
            return MethodDescription.ofMethod(method);
        }
        throw new IllegalStateException(RestClientRequestContext.INVOKED_METHOD_PROP + " property must not be null");
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
