package io.quarkus.deployment.steps;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class ProfileBuildStep {
    @BuildStep
    RunTimeConfigurationDefaultBuildItem defaultProfile(LaunchModeBuildItem launchModeBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem(launchModeBuildItem.getLaunchMode().getProfileKey(),
                ConfigProvider.getConfig().getConfigValue(launchModeBuildItem.getLaunchMode().getProfileKey()).getValue());
    }
}
