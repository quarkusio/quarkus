package org.jboss.resteasy.reactive.client.handlers;

import org.jboss.resteasy.reactive.client.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

/**
 * Simple error handler that fails the result
 */
public class ClientErrorHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (requestContext.getThrowable() != null) {
            requestContext.getResult().completeExceptionally(requestContext.getThrowable());
        }
    }
}
