package io.quarkus.swaggerui.runtime;

import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SwaggerUiRecorder {
    private final RuntimeValue<SwaggerUiRuntimeConfig> runtimeConfig;

    public SwaggerUiRecorder(final RuntimeValue<SwaggerUiRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Handler<RoutingContext> handler(String swaggerUiFinalDestination, String swaggerUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations, ShutdownContext shutdownContext) {
        if (runtimeConfig.getValue().enable()) {
            WebJarStaticHandler handler = new WebJarStaticHandler(swaggerUiFinalDestination, swaggerUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }
}
