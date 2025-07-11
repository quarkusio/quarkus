package io.quarkus.devui.runtime;

import jakarta.enterprise.inject.spi.CDI;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler to return the endpoint info
 */
public class EndpointInfoHandler implements Handler<RoutingContext> {

    private String basePath; // Like /q/dev-ui

    public EndpointInfoHandler() {

    }

    public EndpointInfoHandler(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void handle(RoutingContext event) {
        String normalizedPath = event.normalizedPath();
        if (normalizedPath.contains(SLASH)) {
            int si = normalizedPath.lastIndexOf(SLASH) + 1;
            String path = normalizedPath.substring(0, si);
            String fileName = normalizedPath.substring(si);
            if (path.startsWith(basePath) && fileName.equals("routes.json")) {
                VertxRouteInfoService vertxRouteInfoService = CDI.current().select(VertxRouteInfoService.class).get();
                JsonArray info = vertxRouteInfoService.getInfo();
                event.response()
                        .setStatusCode(STATUS)
                        .setStatusMessage(OK)
                        .putHeader(CONTENT_TYPE, "application/json")
                        .end(Json.encodePrettily(info));
            } else {
                event.next();
            }
        } else {
            event.next();
        }
    }

    private static final int STATUS = 200;
    private static final String OK = "OK";
    private static final String SLASH = "/";
    private static final String CONTENT_TYPE = "Content-Type";
}
