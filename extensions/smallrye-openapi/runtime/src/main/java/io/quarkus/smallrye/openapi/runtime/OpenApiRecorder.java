package io.quarkus.smallrye.openapi.runtime;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OpenApiRecorder {

    public Handler<RoutingContext> handler(OpenApiRuntimeConfig runtimeConfig) {

        if (runtimeConfig.enable) {
            return new OpenApiHandler();
        } else {
            return new OpenApiNotFoundHandler();
        }
    }

    public void setupClDevMode(ShutdownContext shutdownContext) {
        OpenApiConstants.classLoader = Thread.currentThread().getContextClassLoader();
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                OpenApiConstants.classLoader = null;
            }
        });
    }

}
