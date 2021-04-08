package io.quarkus.swaggerui.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Handling static Swagger UI content
 */
public class SwaggerUiStaticHandler implements Handler<RoutingContext> {

    private String swaggerUiFinalDestination;
    private String swaggerUiPath;

    public SwaggerUiStaticHandler() {
    }

    public SwaggerUiStaticHandler(String swaggerUiFinalDestination, String swaggerUiPath) {
        this.swaggerUiFinalDestination = swaggerUiFinalDestination;
        this.swaggerUiPath = swaggerUiPath;
    }

    public String getSwaggerUiFinalDestination() {
        return swaggerUiFinalDestination;
    }

    public void setSwaggerUiFinalDestination(String swaggerUiFinalDestination) {
        this.swaggerUiFinalDestination = swaggerUiFinalDestination;
    }

    public String getSwaggerUiPath() {
        return swaggerUiPath;
    }

    public void setSwaggerUiPath(String swaggerUiPath) {
        this.swaggerUiPath = swaggerUiPath;
    }

    @Override
    public void handle(RoutingContext event) {
        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(swaggerUiFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        if (event.normalizedPath().length() == swaggerUiPath.length()) {
            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, swaggerUiPath + "/");
            event.response().end();
            return;
        } else if (event.normalizedPath().length() == swaggerUiPath.length() + 1) {
            event.reroute(swaggerUiPath + "/index.html");
            return;
        }

        staticHandler.handle(event);
    }

}
