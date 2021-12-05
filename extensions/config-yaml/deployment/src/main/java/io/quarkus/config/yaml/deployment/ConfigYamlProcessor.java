package io.quarkus.config.yaml.deployment;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.config.yaml.runtime.ApplicationYamlConfigSourceLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalBootstrapConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigSourceProviderBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CONFIG_YAML);
    }

    @BuildStep
    public void bootstrap(
            BuildProducer<AdditionalBootstrapConfigSourceProviderBuildItem> additionalBootstrapConfigSourceProvider,
            BuildProducer<StaticInitConfigSourceProviderBuildItem> staticInitConfigSourceProvider) {
        additionalBootstrapConfigSourceProvider.produce(new AdditionalBootstrapConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InFileSystem.class.getName()));
        additionalBootstrapConfigSourceProvider.produce(new AdditionalBootstrapConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InClassPath.class.getName()));
        staticInitConfigSourceProvider.produce(new StaticInitConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InFileSystem.class.getName()));
        staticInitConfigSourceProvider.produce(new StaticInitConfigSourceProviderBuildItem(
                ApplicationYamlConfigSourceLoader.InClassPath.class.getName()));
    }

    @BuildStep
    void watchYamlConfig(BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        List<String> configWatchedFiles = new ArrayList<>();
        String userDir = System.getProperty("user.dir");

        // Main files
        configWatchedFiles.add("application.yaml");
        configWatchedFiles.add("application.yml");
        configWatchedFiles.add(Paths.get(userDir, "config", "application.yaml").toAbsolutePath().toString());
        configWatchedFiles.add(Paths.get(userDir, "config", "application.yml").toAbsolutePath().toString());

        // Profiles
        String profile = ProfileManager.getActiveProfile();
        configWatchedFiles.add(String.format("application-%s.yaml", profile));
        configWatchedFiles.add(String.format("application-%s.yml", profile));
        configWatchedFiles
                .add(Paths.get(userDir, "config", String.format("application-%s.yaml", profile)).toAbsolutePath().toString());
        configWatchedFiles
                .add(Paths.get(userDir, "config", String.format("application-%s.yml", profile)).toAbsolutePath().toString());

        for (String configWatchedFile : configWatchedFiles) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(configWatchedFile));
        }
    }
}
