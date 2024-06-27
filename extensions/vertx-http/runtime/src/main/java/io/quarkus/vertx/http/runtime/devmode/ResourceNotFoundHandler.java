package io.quarkus.vertx.http.runtime.devmode;

import jakarta.enterprise.inject.spi.CDI;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

/**
 * Lists all routes when no route matches the path in the dev mode.
 */
public class ResourceNotFoundHandler implements Handler<RoutingContext> {
    private final ResourceNotFoundData resourceNotFoundData;

    public ResourceNotFoundHandler() {
        this.resourceNotFoundData = CDI.current().select(ResourceNotFoundData.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {

        String header = routingContext.request().getHeader("Accept");
        if (header != null && header.startsWith("application/json")) {
            handleJson(routingContext);
        } else {
            handleHTML(routingContext);
        }
    }

    private void handleJson(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(resourceNotFoundData.getJsonContent()));
    }

    private void handleHTML(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "text/html; charset=utf-8")
                .end(resourceNotFoundData.getHTMLContent());
    }
}
