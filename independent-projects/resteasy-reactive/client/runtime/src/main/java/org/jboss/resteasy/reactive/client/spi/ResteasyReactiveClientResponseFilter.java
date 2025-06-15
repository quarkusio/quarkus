package org.jboss.resteasy.reactive.client.spi;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

/**
 * An extension interface implemented by client response filters used by REST Client Reactive.
 */
public interface ResteasyReactiveClientResponseFilter extends ClientResponseFilter {

    /**
     * Filter method called after a response has been provided for a request (either by a request filter or when the
     * HTTP invocation returns).
     *
     * @param requestContext
     *        the request context.
     * @param responseContext
     *        the response context.
     */
    default void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        filter((ResteasyReactiveClientRequestContext) requestContext, responseContext);
    }

    /**
     * Filter method called after a response has been provided for a request (either by a request filter or when the
     * HTTP invocation returns).
     *
     * @param requestContext
     *        the REST Client reactive request context.
     * @param responseContext
     *        the response context.
     */
    void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext);
}
