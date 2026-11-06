package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.smallrye.config.Config;

public class ProfileBuildStep {
    @BuildStep
    RunTimeConfigurationDefaultBuildItem defaultProfile(LaunchModeBuildItem launchModeBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem(launchModeBuildItem.getLaunchMode().getProfileKey(),
                Config.get().getConfigValue(launchModeBuildItem.getLaunchMode().getProfileKey()).getValue());
    }
}
