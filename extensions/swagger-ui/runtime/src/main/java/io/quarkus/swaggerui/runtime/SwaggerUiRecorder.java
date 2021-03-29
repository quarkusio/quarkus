package io.quarkus.swaggerui.runtime;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SwaggerUiRecorder {

    public Handler<RoutingContext> handler(String swaggerUiFinalDestination, String swaggerUiPath,
            SwaggerUiRuntimeConfig runtimeConfig) {

        if (runtimeConfig.enable) {
            return new SwaggerUiStaticHandler(swaggerUiFinalDestination, swaggerUiPath);
        } else {
            return new SwaggerUiNotFoundHandler();
        }
    }
}
