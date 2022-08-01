package org.jboss.resteasy.reactive.client.handlers;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

/**
 * This Handler is invoked before ClientResponseFilters handler. It changes the abort handler chain
 * to a one without the ClientResponseFilterRestHandler's so that the filters are not retriggered in case of failure
 */
public class PreResponseFilterHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        requestContext.setAbortHandlerChain(requestContext.getAbortHandlerChainWithoutResponseFilters());
    }
}
