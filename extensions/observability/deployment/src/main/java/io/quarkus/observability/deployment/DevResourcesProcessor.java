package io.quarkus.observability.deployment;

import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.observability.runtime.DevResourceShutdownRecorder;
import io.quarkus.observability.runtime.DevResourcesConfigBuilder;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevResourcesProcessor.IsEnabled.class)
class DevResourcesProcessor {
    private static final Logger log = LoggerFactory.getLogger(DevResourcesProcessor.class);
    private static final String FEATURE = "devresources";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public RunTimeConfigBuilderBuildItem registerDevResourcesConfigSource() {
        log.info("Adding dev resources config builder");
        return new RunTimeConfigBuilderBuildItem(DevResourcesConfigBuilder.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public DevResourcesBuildItem shutdownDevResources(DevResourceShutdownRecorder recorder, ShutdownContextBuildItem shutdown) {
        recorder.shutdown(shutdown);
        return new DevResourcesBuildItem();
    }

    public static class IsEnabled implements BooleanSupplier {
        ObservabilityConfiguration config;

        public boolean getAsBoolean() {
            return config.devResources() && !config.enabled();
        }
    }

}
