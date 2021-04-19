package org.jboss.resteasy.reactive.client.spi;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public interface ResteasyReactiveClientResponseFilter extends ClientResponseFilter {

    default void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        filter((ResteasyReactiveClientRequestContext) requestContext, responseContext);
    }

    void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext);
}
