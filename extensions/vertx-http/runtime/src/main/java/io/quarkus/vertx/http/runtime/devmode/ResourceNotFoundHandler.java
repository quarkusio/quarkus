package io.quarkus.vertx.http.runtime.devmode;

import java.util.Locale;

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
        } else if (header != null && header.startsWith("text/html")) {
            handleHTML(routingContext);
        } else {
            // If not explicitly asked for json/html, let determine based on the user agent
            String userAgent = routingContext.request().getHeader("User-Agent");
            if (userAgent != null && (userAgent.toLowerCase(Locale.ROOT).startsWith("wget/")
                    || userAgent.toLowerCase(Locale.ROOT).startsWith("curl/"))) {
                handleText(routingContext);
            } else {
                handleHTML(routingContext);
            }
        }
    }

    private void handleJson(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(resourceNotFoundData.getJsonContent()));
    }

    private void handleText(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "text/plain; charset=utf-8")
                .end(resourceNotFoundData.getTextContent());
    }

    private void handleHTML(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "text/html; charset=utf-8")
                .end(resourceNotFoundData.getHTMLContent());
    }
}
