package io.quarkus.swaggerui.runtime;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.ThreadLocalHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class SwaggerUiRecorder {
    public Handler<RoutingContext> handler(String swaggerUiFinalDestination, String swaggerUiPath) {

        Handler<RoutingContext> handler = new ThreadLocalHandler(new Supplier<Handler<RoutingContext>>() {
            @Override
            public Handler<RoutingContext> get() {
                return StaticHandler.create().setAllowRootFileSystemAccess(true)
                        .setWebRoot(swaggerUiFinalDestination)
                        .setDefaultContentEncoding("UTF-8");
            }
        });

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.normalisedPath().length() == swaggerUiPath.length()) {

                    event.response().setStatusCode(302);
                    event.response().headers().set(HttpHeaders.LOCATION, swaggerUiPath + "/");
                    event.response().end();
                    return;
                } else if (event.normalisedPath().length() == swaggerUiPath.length() + 1) {
                    event.reroute(swaggerUiPath + "/index.html");
                    return;
                }

                handler.handle(event);
            }
        };
    }
}
