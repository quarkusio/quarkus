package io.quarkus.swaggerui.runtime;

import java.util.List;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SwaggerUiRecorder {

    public Handler<RoutingContext> handler(String swaggerUiFinalDestination, String swaggerUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            SwaggerUiRuntimeConfig runtimeConfig, ShutdownContext shutdownContext) {

        if (runtimeConfig.enable()) {
            WebJarStaticHandler handler = new WebJarStaticHandler(swaggerUiFinalDestination, swaggerUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }
}
