package io.quarkus.oidc.client.reactive.filter.runtime;

import static io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter.REQUEST_FILTER_KEY;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

public final class DetectUnauthorizedClientResponseFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() == 401 &&
                requestContext
                        .getProperty(REQUEST_FILTER_KEY) instanceof AbstractOidcClientRequestReactiveFilter requestFilter) {
            requestFilter.refreshAccessToken();
        }
    }
}
