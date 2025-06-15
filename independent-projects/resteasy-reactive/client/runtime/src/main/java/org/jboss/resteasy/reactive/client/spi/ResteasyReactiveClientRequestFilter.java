package org.jboss.resteasy.reactive.client.spi;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * An extension interface implemented by client request filters used by REST Client Reactive.
 */
public interface ResteasyReactiveClientRequestFilter extends ClientRequestFilter {

    /**
     * Filter method called before a request has been dispatched to a client transport layer.
     *
     * @param requestContext
     *        the request context.
     *
     * @throws IOException
     *         if an I/O exception occurs.
     */
    @Override
    default void filter(ClientRequestContext requestContext) throws IOException {
        filter((ResteasyReactiveClientRequestContext) requestContext);
    }

    /**
     * Filter method called before a request has been dispatched to a client transport layer.
     *
     * @param requestContext
     *        the REST Client reactive request context.
     */
    void filter(ResteasyReactiveClientRequestContext requestContext);
}
