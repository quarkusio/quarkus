package io.quarkus.oidc.client.reactive.filter;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.common.runtime.OidcConstants;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestReactiveFilter extends AbstractTokensProducer implements ResteasyReactiveClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(OidcClientRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client-reactive-filter.client-name")
    Optional<String> clientName;

    protected void initTokens() {
        if (earlyTokenAcquisition) {
            LOG.debug("Token acquisition will be delayed until this filter is executed to avoid blocking an IO thread");
        }
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.suspend();

        super.getTokens().subscribe().with(new Consumer<Tokens>() {
            @Override
            public void accept(Tokens tokens) {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + tokens.getAccessToken());
                requestContext.resume();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                if (t instanceof DisabledOidcClientException) {
                    LOG.debug("Client is disabled");
                    requestContext.abortWith(Response.status(500).build());
                } else {
                    LOG.debugf("Access token is not available, aborting the request with HTTP 401 error: %s", t.getMessage());
                    requestContext.abortWith(Response.status(401).build());
                }
                requestContext.resume();
            }
        });
    }

    protected Optional<String> clientId() {
        return clientName;
    }
}
