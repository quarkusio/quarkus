package org.jboss.resteasy.reactive.client.handlers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

/**
 * Simple error handler that fails the result
 */
public class ClientErrorHandler implements ClientRestHandler {
    private static final Logger log = Logger.getLogger(ClientErrorHandler.class);

    private final LoggingScope loggingScope;

    public ClientErrorHandler(LoggingScope loggingScope) {
        this.loggingScope = loggingScope;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (requestContext.getThrowable() != null) {
            if (loggingScope != LoggingScope.NONE) {
                log.debugf(requestContext.getThrowable(), "Failure: %s %s, Error[%s]", requestContext.getHttpMethod(),
                        requestContext.getUri(), requestContext.getThrowable().getMessage());
            }
            requestContext.getResult().completeExceptionally(requestContext.getThrowable());
        }
    }
}
