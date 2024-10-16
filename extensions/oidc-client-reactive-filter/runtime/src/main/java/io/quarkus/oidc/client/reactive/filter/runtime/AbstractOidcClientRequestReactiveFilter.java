package io.quarkus.oidc.client.reactive.filter.runtime;

import java.util.function.Consumer;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.common.runtime.OidcConstants;

public class AbstractOidcClientRequestReactiveFilter extends AbstractTokensProducer
        implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(AbstractOidcClientRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";

    protected void initTokens() {
        if (earlyTokenAcquisition) {
            LOG.debug("Token acquisition will be delayed until this filter is executed to avoid blocking an IO thread");
        }
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (isClientFeatureDisabled()) {
            LOG.debug("OIDC client filter can not acquire and propagate tokens because "
                    + "OIDC client is disabled with `quarkus.oidc-client.enabled=false`");
            return;
        }
        requestContext.suspend();

        super.getTokens().subscribe().with(new Consumer<>() {
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

}
