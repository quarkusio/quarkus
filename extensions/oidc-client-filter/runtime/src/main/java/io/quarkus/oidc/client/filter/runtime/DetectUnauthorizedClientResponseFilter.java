package io.quarkus.oidc.client.filter.runtime;

import static io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter.REQUEST_FILTER_KEY;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

public final class DetectUnauthorizedClientResponseFilter implements ClientResponseFilter {

    public DetectUnauthorizedClientResponseFilter() {
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)
            throws IOException {
        if (clientResponseContext.getStatus() == 401 &&
                clientRequestContext.getProperty(REQUEST_FILTER_KEY) instanceof AbstractOidcClientRequestFilter requestFilter) {
            requestFilter.refreshAccessToken();
        }
    }
}
