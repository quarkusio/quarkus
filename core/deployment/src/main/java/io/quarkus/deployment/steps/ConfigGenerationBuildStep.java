package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalBootstrapConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.configuration.ConfigChangeRecorder;
import io.quarkus.runtime.configuration.ConfigurationRuntimeConfig;

public class ConfigGenerationBuildStep {

    /**
     * Generate the Config class that instantiates MP Config and holds all the config objects
     */
    @BuildStep
    void generateConfigClass(ConfigurationBuildItem configItem, List<RunTimeConfigurationDefaultBuildItem> runTimeDefaults,
            List<ConfigurationTypeBuildItem> typeItems,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem,
            List<AdditionalBootstrapConfigSourceProviderBuildItem> additionalBootstrapConfigSourceProviders) {
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
                launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT, defaults, additionalConfigTypes,
                getAdditionalBootstrapConfigSourceProviders(additionalBootstrapConfigSourceProviders));
    }

    private List<String> getAdditionalBootstrapConfigSourceProviders(
            List<AdditionalBootstrapConfigSourceProviderBuildItem> additionalBootstrapConfigSourceProviders) {
        if (additionalBootstrapConfigSourceProviders.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(additionalBootstrapConfigSourceProviders.size());
        for (AdditionalBootstrapConfigSourceProviderBuildItem provider : additionalBootstrapConfigSourceProviders) {
            result.add(provider.getProviderClassName());
        }
        return result;
    }

    /**
     * Warns if build time config properties have been changed at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void checkForBuildTimeConfigChange(
            ConfigChangeRecorder recorder, ConfigurationBuildItem configItem, LoggingSetupBuildItem loggingSetupBuildItem,
            ConfigurationRuntimeConfig configurationConfig) {
        BuildTimeConfigurationReader.ReadResult readResult = configItem.getReadResult();
        Config config = ConfigProvider.getConfig();

        Map<String, String> values = new HashMap<>();
        for (RootDefinition root : readResult.getAllRoots()) {
            if (root.getConfigPhase() == ConfigPhase.BUILD_AND_RUN_TIME_FIXED ||
                    root.getConfigPhase() == ConfigPhase.BUILD_TIME) {

                Iterable<ClassDefinition.ClassMember> members = root.getMembers();
                handleMembers(config, values, members, "quarkus." + root.getRootName() + ".");
            }
        }
        values.remove("quarkus.profile");
        recorder.handleConfigChange(configurationConfig, values);
    }

    private void handleMembers(Config config, Map<String, String> values, Iterable<ClassDefinition.ClassMember> members,
            String prefix) {
        for (ClassDefinition.ClassMember member : members) {
            if (member instanceof ClassDefinition.ItemMember) {
                ClassDefinition.ItemMember itemMember = (ClassDefinition.ItemMember) member;
                String propertyName = prefix + member.getPropertyName();
                Optional<String> val = config.getOptionalValue(propertyName, String.class);
                if (val.isPresent()) {
                    values.put(propertyName, val.get());
                } else {
                    values.put(propertyName, itemMember.getDefaultValue());
                }
            } else if (member instanceof ClassDefinition.GroupMember) {
                handleMembers(config, values, ((ClassDefinition.GroupMember) member).getGroupDefinition().getMembers(),
                        prefix + member.getDescriptor().getName() + ".");
            }
        }
    }

}
