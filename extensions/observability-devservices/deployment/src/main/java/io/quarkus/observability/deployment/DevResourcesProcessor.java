package io.quarkus.observability.deployment;

import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.IsDevResourcesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.observability.devresource.DevResources;
import io.quarkus.observability.runtime.DevResourcesConfigBuilder;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIf = { IsDevResourcesSupportedByLaunchMode.class, DevResourcesProcessor.IsEnabled.class })
class DevResourcesProcessor {
    private static final Logger log = LoggerFactory.getLogger(DevResourcesProcessor.class);
    private static final String FEATURE = "devresources";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void registerDevResourcesConfigSource(LaunchModeBuildItem launchMode,
            ObservabilityConfiguration configuration,
            BuildProducer<RunTimeConfigBuilderBuildItem> configBuilder) {
        if (launchMode.getLaunchMode() == LaunchMode.TEST && !configuration.enabledInTests()) {
            log.info("Dev resources are disabled in test mode by default. "
                    + "Set quarkus.observability.enabled-in-tests=true to enable.");
            return;
        }
        log.info("Adding dev resources config builder");
        configBuilder.produce(new RunTimeConfigBuilderBuildItem(DevResourcesConfigBuilder.class));
    }

    @BuildStep
    public DevResourcesBuildItem shutdownDevResources(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.observability.dev-resources.shutdown")
                .onStart(ctx -> ctx.onStop(DevResources::stop));
        return new DevResourcesBuildItem();
    }

    public static class IsEnabled implements BooleanSupplier {
        ObservabilityConfiguration config;

        public boolean getAsBoolean() {
            return config.devResources() && !config.enabled();
        }
    }

}
