package io.quarkus.deployment.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.ConfigBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.configuration.SystemOnlySourcesConfigBuilder;
import io.quarkus.runtime.graal.InetRunTime;

class ConfigBuildSteps {
    static final String SERVICES_PREFIX = "META-INF/services/";

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem(InetRunTime.class.getName());
    }

    @BuildStep(onlyIf = SystemOnlySources.class)
    void systemOnlySources(BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(SystemOnlySourcesConfigBuilder.class.getName()));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(SystemOnlySourcesConfigBuilder.class.getName()));
    }

    private static class SystemOnlySources implements BooleanSupplier {
        ConfigBuildTimeConfig configBuildTimeConfig;

        @Override
        public boolean getAsBoolean() {
            return configBuildTimeConfig.systemOnly();
        }
    }
}
