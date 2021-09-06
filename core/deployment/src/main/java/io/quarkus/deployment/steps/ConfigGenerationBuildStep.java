package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.ConfigBuildSteps.SERVICES_PREFIX;
import static io.quarkus.deployment.util.ServiceUtil.classNamesNamedIn;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalBootstrapConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.AdditionalStaticInitConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigSourceFactoryBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.runtime.configuration.ConfigChangeRecorder;
import io.quarkus.runtime.configuration.ConfigurationRuntimeConfig;
import io.quarkus.runtime.configuration.RuntimeOverrideConfigSource;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.PropertiesLocationConfigSourceFactory;

public class ConfigGenerationBuildStep {
    @BuildStep
    void deprecatedStaticInitBuildItem(
            List<AdditionalStaticInitConfigSourceProviderBuildItem> additionalStaticInitConfigSourceProviders,
            BuildProducer<StaticInitConfigSourceProviderBuildItem> staticInitConfigSourceProviderBuildItem) {
        for (AdditionalStaticInitConfigSourceProviderBuildItem item : additionalStaticInitConfigSourceProviders) {
            staticInitConfigSourceProviderBuildItem
                    .produce(new StaticInitConfigSourceProviderBuildItem(item.getProviderClassName()));
        }
    }

    @BuildStep
    void staticInitSources(
            BuildProducer<StaticInitConfigSourceProviderBuildItem> staticInitConfigSourceProviderBuildItem,
            BuildProducer<StaticInitConfigSourceFactoryBuildItem> staticInitConfigSourceFactoryBuildItem) {

        staticInitConfigSourceFactoryBuildItem.produce(new StaticInitConfigSourceFactoryBuildItem(
                PropertiesLocationConfigSourceFactory.class.getName()));
    }

    /**
     * Generate the Config class that instantiates MP Config and holds all the config objects
     */
    @BuildStep
    void generateConfigClass(
            ConfigurationBuildItem configItem,
            List<RunTimeConfigurationDefaultBuildItem> runTimeDefaults,
            List<ConfigurationTypeBuildItem> typeItems,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            LiveReloadBuildItem liveReloadBuildItem,
            List<AdditionalBootstrapConfigSourceProviderBuildItem> additionalBootstrapConfigSourceProviders,
            List<StaticInitConfigSourceProviderBuildItem> staticInitConfigSourceProviders,
            List<StaticInitConfigSourceFactoryBuildItem> staticInitConfigSourceFactories)
            throws IOException {

        if (liveReloadBuildItem.isLiveReload()) {
            return;
        }

        Map<String, String> defaults = new HashMap<>();
        for (RunTimeConfigurationDefaultBuildItem item : runTimeDefaults) {
            if (defaults.putIfAbsent(item.getKey(), item.getValue()) != null) {
                throw new IllegalStateException("More than one default value for " + item.getKey() + " was produced");
            }
        }

        Set<String> discoveredConfigSources = discoverService(ConfigSource.class, reflectiveClass);
        Set<String> discoveredConfigSourceProviders = discoverService(ConfigSourceProvider.class, reflectiveClass);
        Set<String> discoveredConfigSourceFactories = discoverService(ConfigSourceFactory.class, reflectiveClass);

        Set<String> staticConfigSourceProviders = new HashSet<>();
        staticConfigSourceProviders.addAll(staticSafeServices(discoveredConfigSourceProviders));
        staticConfigSourceProviders.addAll(staticInitConfigSourceProviders.stream()
                .map(StaticInitConfigSourceProviderBuildItem::getProviderClassName).collect(Collectors.toSet()));
        Set<String> staticConfigSourceFactories = new HashSet<>();
        staticConfigSourceFactories.addAll(staticSafeServices(discoveredConfigSourceFactories));
        staticConfigSourceFactories.addAll(staticInitConfigSourceFactories.stream()
                .map(StaticInitConfigSourceFactoryBuildItem::getFactoryClassName).collect(Collectors.toSet()));

        RunTimeConfigurationGenerator.GenerateOperation
                .builder()
                .setBuildTimeReadResult(configItem.getReadResult())
                .setClassOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .setLaunchMode(launchModeBuildItem.getLaunchMode())
                .setRunTimeDefaults(defaults)
                .setAdditionalTypes(typeItems.stream().map(ConfigurationTypeBuildItem::getValueType).collect(toList()))
                .setAdditionalBootstrapConfigSourceProviders(
                        getAdditionalBootstrapConfigSourceProviders(additionalBootstrapConfigSourceProviders))
                .setStaticConfigSources(staticSafeServices(discoveredConfigSources))
                .setStaticConfigSourceProviders(staticConfigSourceProviders)
                .setStaticConfigSourceFactories(staticConfigSourceFactories)
                .setRuntimeConfigSources(discoveredConfigSources)
                .setRuntimeConfigSourceProviders(discoveredConfigSourceProviders)
                .setRuntimeConfigSourceFactories(discoveredConfigSourceFactories)
                .build()
                .run();
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

    @BuildStep
    public SuppressNonRuntimeConfigChangedWarningBuildItem ignoreQuarkusProfileChange() {
        return new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.profile");
    }

    /**
     * Warns if build time config properties have been changed at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void checkForBuildTimeConfigChange(
            ConfigChangeRecorder recorder, ConfigurationBuildItem configItem, LoggingSetupBuildItem loggingSetupBuildItem,
            ConfigurationRuntimeConfig configurationConfig,
            List<SuppressNonRuntimeConfigChangedWarningBuildItem> suppressNonRuntimeConfigChangedWarningItems) {
        BuildTimeConfigurationReader.ReadResult readResult = configItem.getReadResult();
        Config config = ConfigProvider.getConfig();

        Set<String> excludedConfigKeys = new HashSet<>(suppressNonRuntimeConfigChangedWarningItems.size());
        for (SuppressNonRuntimeConfigChangedWarningBuildItem item : suppressNonRuntimeConfigChangedWarningItems) {
            excludedConfigKeys.add(item.getConfigKey());
        }

        Map<String, String> values = new HashMap<>();
        for (RootDefinition root : readResult.getAllRoots()) {
            if (root.getConfigPhase() == ConfigPhase.BUILD_AND_RUN_TIME_FIXED ||
                    root.getConfigPhase() == ConfigPhase.BUILD_TIME) {

                Iterable<ClassDefinition.ClassMember> members = root.getMembers();
                handleMembers(config, values, members, "quarkus." + root.getRootName() + ".", excludedConfigKeys);
            }
        }
        recorder.handleConfigChange(configurationConfig, values);
    }

    @BuildStep(onlyIfNot = { IsNormal.class })
    public void setupConfigOverride(
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true);

        try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                .className(RuntimeOverrideConfigSource.GENERATED_CLASS_NAME).build()) {
            clazz.getFieldCreator(RuntimeOverrideConfigSource.FIELD_NAME, Map.class)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC | Modifier.VOLATILE);
        }
    }

    private void handleMembers(Config config, Map<String, String> values, Iterable<ClassDefinition.ClassMember> members,
            String prefix, Set<String> excludedConfigKeys) {
        for (ClassDefinition.ClassMember member : members) {
            if (member instanceof ClassDefinition.ItemMember) {
                ClassDefinition.ItemMember itemMember = (ClassDefinition.ItemMember) member;
                String propertyName = prefix + member.getPropertyName();
                if (excludedConfigKeys.contains(propertyName)) {
                    continue;
                }
                Optional<String> val = config.getOptionalValue(propertyName, String.class);
                if (val.isPresent()) {
                    values.put(propertyName, val.get());
                } else {
                    values.put(propertyName, itemMember.getDefaultValue());
                }
            } else if (member instanceof ClassDefinition.GroupMember) {
                handleMembers(config, values, ((ClassDefinition.GroupMember) member).getGroupDefinition().getMembers(),
                        prefix + member.getDescriptor().getName() + ".", excludedConfigKeys);
            }
        }
    }

    private static Set<String> discoverService(
            Class<?> serviceClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> services = new HashSet<>();
        for (String service : classNamesNamedIn(classLoader, SERVICES_PREFIX + serviceClass.getName())) {
            services.add(service);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, service));
        }
        return services;
    }

    private static Set<String> staticSafeServices(Set<String> services) {
        // TODO - Replace with Jandex? The issue is that the sources may not be in the index...
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Set<String> staticSafe = new HashSet<>();
        for (String service : services) {
            try {
                Class<?> serviceClass = classloader.loadClass(service);
                if (serviceClass.isAnnotationPresent(StaticInitSafe.class)) {
                    staticSafe.add(service);
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        return staticSafe;
    }
}
