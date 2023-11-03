package io.quarkus.deployment.steps;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Set;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.configuration.DeprecatedRuntimePropertiesRecorder;

public class DeprecatedRuntimePropertiesBuildStep {

    @BuildStep(onlyIf = IsNormal.class)
    @Record(RUNTIME_INIT)
    void reportDeprecatedProperties(LaunchModeBuildItem launchMode, ConfigurationBuildItem configItem,
            DeprecatedRuntimePropertiesRecorder recorder) {
        Set<String> deprecatedProperties = configItem.getReadResult().getDeprecatedRuntimeProperties();
        if (!deprecatedProperties.isEmpty()) {
            recorder.reportDeprecatedProperties(deprecatedProperties);
        }
    }

}
