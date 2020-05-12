package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.LaunchMode;

public class ProfileBuildStep {

    @BuildStep
    RunTimeConfigurationDefaultBuildItem defaultProfile(LaunchModeBuildItem launchModeBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem("quarkus.profile",
                getProfileValue(launchModeBuildItem.getLaunchMode()));
    }

    private String getProfileValue(LaunchMode launchMode) {
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return "dev";
        } else if (launchMode == LaunchMode.TEST) {
            return "test";
        }
        return "prod";
    }
}
