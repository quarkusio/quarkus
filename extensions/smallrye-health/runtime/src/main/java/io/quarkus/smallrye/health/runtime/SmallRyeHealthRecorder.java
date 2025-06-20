package io.quarkus.smallrye.health.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
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
    private final SmallRyeHealthBuildFixedConfig buildFixedConfig;
    private final RuntimeValue<SmallRyeHealthRuntimeConfig> runtimeConfig;

    public SmallRyeHealthRecorder(
            final SmallRyeHealthBuildFixedConfig buildFixedConfig,
            final RuntimeValue<SmallRyeHealthRuntimeConfig> runtimeConfig) {
        this.buildFixedConfig = buildFixedConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

    public Handler<RoutingContext> uiHandler(String healthUiFinalDestination, String healthUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations, ShutdownContext shutdownContext) {

        if (runtimeConfig.getValue().enable()) {
            WebJarStaticHandler handler = new WebJarStaticHandler(healthUiFinalDestination, healthUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }

    public void processSmallRyeHealthRuntimeConfiguration() {
        SmallRyeHealthReporter reporter = Arc.container().select(SmallRyeHealthReporter.class).get();
        reporter.setAdditionalProperties(runtimeConfig.getValue().additionalProperties());

        reporter.setHealthChecksConfigs(runtimeConfig.getValue().check().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().enabled())));

        SmallRyeHealthHandlerBase.problemDetails = buildFixedConfig.includeProblemDetails();
    }

}
