package io.quarkus.oidc.client.filter;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
    @ConfigProperty(name = "quarkus.oidc-client-filter.client-name")
    Optional<String> clientName;

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
        return clientName;
    }
}
