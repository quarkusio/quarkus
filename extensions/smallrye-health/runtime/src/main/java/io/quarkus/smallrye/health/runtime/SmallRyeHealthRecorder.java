package io.quarkus.smallrye.health.runtime;

import java.util.List;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SmallRyeHealthRecorder {

    public void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

    public Handler<RoutingContext> uiHandler(String healthUiFinalDestination, String healthUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            SmallRyeHealthRuntimeConfig runtimeConfig, ShutdownContext shutdownContext) {

        if (runtimeConfig.enable) {
            WebJarStaticHandler handler = new WebJarStaticHandler(healthUiFinalDestination, healthUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }
}
