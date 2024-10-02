package io.quarkus.smallrye.health.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.smallrye.health.SmallRyeHealthReporter;
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

    public void processSmallRyeHealthRuntimeConfiguration(SmallRyeHealthRuntimeConfig runtimeConfig,
            SmallRyeHealthBuildFixedConfig buildFixedConfig) {
        SmallRyeHealthReporter reporter = Arc.container().select(SmallRyeHealthReporter.class).get();
        reporter.setAdditionalProperties(runtimeConfig.additionalProperties);

        reporter.setHealthChecksConfigs(runtimeConfig.check.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().enabled)));

        SmallRyeHealthHandlerBase.problemDetails = buildFixedConfig.includeProblemDetails;
    }

}
