package org.jboss.resteasy.reactive.client.handlers;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

/**
 * This is added by the Reactive Rest Client if observability features are enabled
 */
@SuppressWarnings("unused")
public class ClientObservabilityHandler implements ClientRestHandler {

    private final String templatePath;

    public ClientObservabilityHandler(String templatePath) {
        this.templatePath = templatePath;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        requestContext.getClientFilterProperties().put("UrlPathTemplate", templatePath);
    }
}
