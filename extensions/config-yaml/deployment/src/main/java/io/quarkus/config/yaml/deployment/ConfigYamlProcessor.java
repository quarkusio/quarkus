package io.quarkus.config.yaml.deployment;

import io.quarkus.config.yaml.runtime.ApplicationYamlProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CONFIG_YAML);
    }

    @BuildStep
    void watchYamlConfig(BuildProducer<HotDeploymentWatchedFileBuildItem> items) {
        items.produce(new HotDeploymentWatchedFileBuildItem(ApplicationYamlProvider.APPLICATION_YAML));
        items.produce(new HotDeploymentWatchedFileBuildItem(ApplicationYamlProvider.APPLICATION_YML));
    }
}
