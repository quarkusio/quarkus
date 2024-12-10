package io.quarkus.config.yaml.deployment;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.config.yaml.runtime.NativeYamlConfigBuilder;
import io.quarkus.config.yaml.runtime.YamlConfigBuilder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.smallrye.config.SmallRyeConfig;

public final class ConfigYamlProcessor {

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CONFIG_YAML);
    }

    @BuildStep(onlyIfNot = NativeOrNativeSourcesBuild.class)
    public void yamlConfig(
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(YamlConfigBuilder.class));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(YamlConfigBuilder.class));
    }

    /**
     * Limit external configuration sources parsing for native executables since they are not supported see
     * https://github.com/quarkusio/quarkus/issues/41994
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeNoSources(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(NativeYamlConfigBuilder.class));
    }

    @BuildStep
    void watchYamlConfig(BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        List<String> configWatchedFiles = new ArrayList<>();
        String userDir = System.getProperty("user.dir");

        // Main files
        configWatchedFiles.add("META-INF/microprofile-config.yaml");
        configWatchedFiles.add("META-INF/microprofile-config.yml");
        configWatchedFiles.add("application.yaml");
        configWatchedFiles.add("application.yml");
        configWatchedFiles.add(Paths.get(userDir, "config", "application.yaml").toAbsolutePath().toString());
        configWatchedFiles.add(Paths.get(userDir, "config", "application.yml").toAbsolutePath().toString());

        // Profiles
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (String profile : config.getProfiles()) {
            configWatchedFiles.add(String.format("application-%s.yaml", profile));
            configWatchedFiles.add(String.format("application-%s.yml", profile));
            configWatchedFiles.add(
                    Paths.get(userDir, "config", String.format("application-%s.yaml", profile)).toAbsolutePath().toString());
            configWatchedFiles.add(
                    Paths.get(userDir, "config", String.format("application-%s.yml", profile)).toAbsolutePath().toString());
        }

        for (String configWatchedFile : configWatchedFiles) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(configWatchedFile));
        }
    }

    /**
     * Registers configuration files for access at runtime in native-mode. Doesn't use absolute paths.
     */
    @BuildStep
    NativeImageResourceBuildItem nativeYamlConfig() {
        List<String> configFiles = new ArrayList<>();

        // Main files
        configFiles.add("META-INF/microprofile-config.yaml");
        configFiles.add("META-INF/microprofile-config.yml");
        configFiles.add("application.yaml");
        configFiles.add("application.yml");
        configFiles.add(Paths.get("config", "application.yaml").toString());
        configFiles.add(Paths.get("config", "application.yml").toString());

        // Profiles
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (String profile : config.getProfiles()) {
            configFiles.add(String.format("application-%s.yaml", profile));
            configFiles.add(String.format("application-%s.yml", profile));
            configFiles.add(Paths.get("config", String.format("application-%s.yaml", profile)).toString());
            configFiles.add(Paths.get("config", String.format("application-%s.yml", profile)).toString());
        }

        return new NativeImageResourceBuildItem(configFiles);
    }
}
