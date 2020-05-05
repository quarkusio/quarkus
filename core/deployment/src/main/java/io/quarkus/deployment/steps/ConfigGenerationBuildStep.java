package io.quarkus.deployment.steps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.runtime.LaunchMode;

public class ConfigGenerationBuildStep {

    /**
     * Generate the Config class that instantiates MP Config and holds all the config objects
     */
    @BuildStep
    void generateConfigClass(ConfigurationBuildItem configItem, List<RunTimeConfigurationDefaultBuildItem> runTimeDefaults,
            List<ConfigurationTypeBuildItem> typeItems,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem) {
        if (liveReloadBuildItem.isLiveReload()) {
            return;
        }
        BuildTimeConfigurationReader.ReadResult readResult = configItem.getReadResult();
        Map<String, String> defaults = new HashMap<>();
        for (RunTimeConfigurationDefaultBuildItem item : runTimeDefaults) {
            if (defaults.putIfAbsent(item.getKey(), item.getValue()) != null) {
                throw new IllegalStateException("More than one default value for " + item.getKey() + " was produced");
            }
        }
        List<Class<?>> additionalConfigTypes = typeItems.stream().map(ConfigurationTypeBuildItem::getValueType)
                .collect(Collectors.toList());

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, false);

        RunTimeConfigurationGenerator.generate(readResult, classOutput,
                launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT, defaults, additionalConfigTypes);
    }
}
