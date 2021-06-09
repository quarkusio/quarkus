package io.quarkus.config.yaml.deployment;

import io.quarkus.config.yaml.runtime.ApplicationYamlConfigSourceLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalBootstrapConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CONFIG_YAML);
    }

    @BuildStep
    public void bootstrap(
            BuildProducer<AdditionalBootstrapConfigSourceProviderBuildItem> additionalBootstrapConfigSourceProvider) {
        additionalBootstrapConfigSourceProvider.produce(new AdditionalBootstrapConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InFileSystem.class.getName()));
        additionalBootstrapConfigSourceProvider.produce(new AdditionalBootstrapConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InClassPath.class.getName()));
    }

    @BuildStep
    void watchYamlConfig(BuildProducer<HotDeploymentWatchedFileBuildItem> items) {
        items.produce(new HotDeploymentWatchedFileBuildItem("application.yaml"));
        items.produce(new HotDeploymentWatchedFileBuildItem("application.yml"));
    }
}
