package io.quarkus.deployment.steps;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSource;

class DevModeBuildStep {

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> config() {
        return Arrays.asList(
                new HotDeploymentWatchedFileBuildItem("META-INF/microprofile-config.properties"),
                new HotDeploymentWatchedFileBuildItem("application.properties"));
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem hotReloadLocalConfigFile() {
        final String localConfigFile = ApplicationPropertiesConfigSource.LocalConfigFile.getLocalConfigFilename().orElse(null);
        if (localConfigFile != null) {
            return new HotDeploymentWatchedFileBuildItem(localConfigFile);
        }

        return null;
    }
}
