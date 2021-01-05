package io.quarkus.config.yaml.deployment;

import io.quarkus.config.yaml.runtime.ApplicationYamlProvider;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalBootstrapConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CONFIG_YAML);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.CONFIG_YAML);
    }

    @BuildStep
    public AdditionalBootstrapConfigSourceProviderBuildItem bootstrap() {
        return new AdditionalBootstrapConfigSourceProviderBuildItem(ApplicationYamlProvider.class.getName());
    }

    @BuildStep
    void watchYamlConfig(BuildProducer<HotDeploymentWatchedFileBuildItem> items) {
        items.produce(new HotDeploymentWatchedFileBuildItem(ApplicationYamlProvider.APPLICATION_YAML));
        items.produce(new HotDeploymentWatchedFileBuildItem(ApplicationYamlProvider.APPLICATION_YML));
    }
}
