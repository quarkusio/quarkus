package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;

@Deprecated(forRemoval = true, since = "3.31")
public class RuntimeConfigSetupBuildStep {
    @BuildStep
    @Produce(RuntimeConfigSetupCompleteBuildItem.class)
    void setupRuntimeConfig(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<MainBytecodeRecorderBuildItem> mainBytecodeRecorder) {
    }
}
