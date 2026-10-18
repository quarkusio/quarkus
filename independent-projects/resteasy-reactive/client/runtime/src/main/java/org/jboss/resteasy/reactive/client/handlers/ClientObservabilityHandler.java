package org.jboss.resteasy.reactive.client.handlers;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;

/**
 * This is added by the Reactive Rest Client if observability features are enabled
 */
@SuppressWarnings("unused")
public class ClientObservabilityHandler implements ClientRestHandler {

    public static final String CLIENT_URL_PATH_TEMPLATE_KEY = "ClientUrlPathTemplate";

    private final String templatePath;

    public ClientObservabilityHandler(String templatePath) {
        this.templatePath = templatePath;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        requestContext.getClientFilterProperties().put("UrlPathTemplate", templatePath);
        if (VertxContext.isOnDuplicatedContext()) {
            ContextLocals.put(CLIENT_URL_PATH_TEMPLATE_KEY, templatePath);
        }
    }
}
