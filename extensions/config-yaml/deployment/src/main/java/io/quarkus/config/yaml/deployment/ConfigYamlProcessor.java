package io.quarkus.config.yaml.deployment;

import io.quarkus.config.yaml.runtime.ApplicationYamlProvider;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CONFIG_YAML);
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchYamlConfig() {
        return new HotDeploymentWatchedFileBuildItem(ApplicationYamlProvider.APPLICATION_YAML);
    }
}
