package io.quarkus.config.yaml.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CONFIG_YAML);
    }
}
