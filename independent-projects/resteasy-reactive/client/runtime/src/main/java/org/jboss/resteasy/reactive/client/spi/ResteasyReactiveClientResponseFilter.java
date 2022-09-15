package org.jboss.resteasy.reactive.client.spi;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

public interface ResteasyReactiveClientResponseFilter extends ClientResponseFilter {

    default void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        filter((ResteasyReactiveClientRequestContext) requestContext, responseContext);
    }

    void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext);
}
