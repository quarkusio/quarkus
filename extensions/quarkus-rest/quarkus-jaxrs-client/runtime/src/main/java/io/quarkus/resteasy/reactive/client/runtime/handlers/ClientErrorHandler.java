package io.quarkus.resteasy.reactive.client.runtime.handlers;

import io.quarkus.resteasy.reactive.client.runtime.ClientRestHandler;
import io.quarkus.resteasy.reactive.client.runtime.RestClientRequestContext;

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
