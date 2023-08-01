package io.quarkus.deployment.steps;

import static io.quarkus.deployment.configuration.ConfigMappingUtils.CONFIG_MAPPING_NAME;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.processConfigClasses;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.processExtensionConfigMapping;
import static io.quarkus.deployment.steps.ConfigBuildSteps.SERVICES_PREFIX;
import static io.quarkus.deployment.util.ServiceUtil.classNamesNamedIn;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.quarkus.runtime.configuration.ConfigDiagnostic;
import io.quarkus.runtime.configuration.ConfigRecorder;
import io.quarkus.runtime.configuration.DefaultsConfigSource;
import io.quarkus.runtime.configuration.DisableableConfigSource;
import io.quarkus.runtime.configuration.MappingsConfigBuilder;
import io.quarkus.runtime.configuration.QuarkusConfigValue;
import io.quarkus.runtime.configuration.RuntimeOverrideConfigSource;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigGenerationBuildStep {
    private static final MethodDescriptor CONFIG_BUILDER = MethodDescriptor.ofMethod(
            ConfigBuilder.class, "configBuilder",
            SmallRyeConfigBuilder.class, SmallRyeConfigBuilder.class);
    private static final MethodDescriptor WITH_SOURCES = MethodDescriptor.ofMethod(
            SmallRyeConfigBuilder.class, "withSources",
            SmallRyeConfigBuilder.class, ConfigSource[].class);

    @BuildStep
    void buildTimeRunTimeConfig(
            ConfigurationBuildItem configItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        String className = "io.quarkus.runtime.generated.BuildTimeRunTimeFixedConfigSource";
        generateDefaultsConfigSource(generatedClass, reflectiveClass, configItem.getReadResult().getBuildTimeRunTimeValues(),
                className, "BuildTime RunTime Fixed", Integer.MAX_VALUE);

        String builderClassName = className + "Builder";
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(builderClassName)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            FieldDescriptor source = FieldDescriptor.of(classCreator.getClassName(), "source", ConfigSource.class);
            classCreator.getFieldCreator(source).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(Opcodes.ACC_STATIC);
            ResultHandle buildTimeRunTimeConfigSource = clinit.newInstance(MethodDescriptor.ofConstructor(className));
            ResultHandle disableableConfigSource = clinit.newInstance(
                    MethodDescriptor.ofConstructor(DisableableConfigSource.class, ConfigSource.class),
                    buildTimeRunTimeConfigSource);
            clinit.writeStaticField(source, disableableConfigSource);
            clinit.returnVoid();

            MethodCreator method = classCreator.getMethodCreator(CONFIG_BUILDER);
            ResultHandle configBuilder = method.getMethodParam(0);

            ResultHandle configSources = method.newArray(ConfigSource.class, 1);
            method.writeArrayValue(configSources, 0, method.readStaticField(source));

            method.invokeVirtualMethod(WITH_SOURCES, configBuilder, configSources);

            method.returnValue(configBuilder);
        }

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(builderClassName).build());
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(builderClassName));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(builderClassName));
    }

    @BuildStep
    void runtimeDefaultsConfig(
            ConfigurationBuildItem configItem,
            List<RunTimeConfigurationDefaultBuildItem> runTimeDefaults,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        Map<String, String> defaults = new HashMap<>();
        for (RunTimeConfigurationDefaultBuildItem e : runTimeDefaults) {
            defaults.put(e.getKey(), e.getValue());
        }
        defaults.putAll(configItem.getReadResult().getRunTimeDefaultValues());

        String className = "io.quarkus.runtime.generated.RunTimeDefaultsConfigSource";
        generateDefaultsConfigSource(generatedClass, reflectiveClass, defaults,
                className, "RunTime Defaults", Integer.MIN_VALUE + 100);

        String builderClassName = className + "Builder";
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(builderClassName)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            FieldDescriptor source = FieldDescriptor.of(classCreator.getClassName(), "source", ConfigSource.class);
            classCreator.getFieldCreator(source).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(Opcodes.ACC_STATIC);
            ResultHandle runtimeDefaultsConfigSource = clinit.newInstance(MethodDescriptor.ofConstructor(className));
            clinit.writeStaticField(source, runtimeDefaultsConfigSource);
            clinit.returnVoid();

            MethodCreator method = classCreator.getMethodCreator(CONFIG_BUILDER);
            ResultHandle configBuilder = method.getMethodParam(0);

            ResultHandle configSources = method.newArray(ConfigSource.class, 1);
            method.writeArrayValue(configSources, 0, method.readStaticField(source));

            method.invokeVirtualMethod(WITH_SOURCES, configBuilder, configSources);

            method.returnValue(configBuilder);
        }

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(builderClassName).build());
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(builderClassName));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(builderClassName));
    }

    @BuildStep
    void mappings(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses) {

        processConfigClasses(combinedIndex, generatedClasses, reflectiveClasses, configClasses, CONFIG_MAPPING_NAME);
    }

    @BuildStep
    void extensionMappings(ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses) {

        List<ConfigClassWithPrefix> buildTimeRunTimeMappings = configItem.getReadResult().getBuildTimeRunTimeMappings();
        for (ConfigClassWithPrefix buildTimeRunTimeMapping : buildTimeRunTimeMappings) {
            processExtensionConfigMapping(buildTimeRunTimeMapping.getKlass(), buildTimeRunTimeMapping.getPrefix(),
                    combinedIndex, generatedClasses, reflectiveClasses, configClasses);
        }

        final List<ConfigClassWithPrefix> runTimeMappings = configItem.getReadResult().getRunTimeMappings();
        for (ConfigClassWithPrefix runTimeMapping : runTimeMappings) {
            processExtensionConfigMapping(runTimeMapping.getKlass(), runTimeMapping.getPrefix(), combinedIndex,
                    generatedClasses, reflectiveClasses, configClasses);
        }
    }

    @BuildStep
    void builderMappings(
            ConfigurationBuildItem configItem,
            List<ConfigMappingBuildItem> configMappings,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        // For Static Init Config
        Set<ConfigClassWithPrefix> staticMappings = new HashSet<>();
        staticMappings.addAll(staticSafeConfigMappings(configMappings));
        staticMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());
        String staticInitMappingsConfigBuilder = "io.quarkus.runtime.generated.StaticInitMappingsConfigBuilder";
        generateMappingsConfigBuilder(generatedClass, reflectiveClass, staticInitMappingsConfigBuilder, staticMappings);
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(staticInitMappingsConfigBuilder));

        // For RunTime Config
        Set<ConfigClassWithPrefix> runTimeMappings = new HashSet<>();
        runTimeMappings.addAll(runtimeConfigMappings(configMappings));
        runTimeMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());
        runTimeMappings.addAll(configItem.getReadResult().getRunTimeMappings());
        String runTimeMappingsConfigBuilder = "io.quarkus.runtime.generated.RunTimeMappingsConfigBuilder";
        generateMappingsConfigBuilder(generatedClass, reflectiveClass, runTimeMappingsConfigBuilder, runTimeMappings);
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(runTimeMappingsConfigBuilder));
    }

    /**
     * Generate the Config class that instantiates MP Config and holds all the config objects
     */
    @BuildStep
    void generateConfigClass(
            ConfigurationBuildItem configItem,
            List<ConfigurationTypeBuildItem> typeItems,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            LiveReloadBuildItem liveReloadBuildItem,
            List<ConfigMappingBuildItem> configMappings,
            List<StaticInitConfigBuilderBuildItem> staticInitConfigBuilders,
            List<RunTimeConfigBuilderBuildItem> runTimeConfigBuilders)
            throws IOException {

        reportUnknownBuildProperties(launchModeBuildItem.getLaunchMode(),
                configItem.getReadResult().getUnknownBuildProperties());

        if (liveReloadBuildItem.isLiveReload()) {
            return;
        }

        Set<String> discoveredConfigSources = discoverService(ConfigSource.class, reflectiveClass);
        Set<String> discoveredConfigSourceProviders = discoverService(ConfigSourceProvider.class, reflectiveClass);
        Set<String> discoveredConfigSourceFactories = discoverService(ConfigSourceFactory.class, reflectiveClass);

        Set<String> staticConfigSourceProviders = staticSafeServices(discoveredConfigSourceProviders);
        Set<String> staticConfigSourceFactories = staticSafeServices(discoveredConfigSourceFactories);

        // TODO - duplicated now builderMappings. Still required to filter the unknown properties
        Set<ConfigClassWithPrefix> staticMappings = new HashSet<>();
        staticMappings.addAll(staticSafeConfigMappings(configMappings));
        staticMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());

        Set<ConfigClassWithPrefix> runtimeMappings = new HashSet<>();
        runtimeMappings.addAll(runtimeConfigMappings(configMappings));
        runtimeMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());
        runtimeMappings.addAll(configItem.getReadResult().getRunTimeMappings());

        Set<String> runtimeConfigBuilderClassNames = runTimeConfigBuilders.stream()
                .map(RunTimeConfigBuilderBuildItem::getBuilderClassName).collect(toSet());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(runtimeConfigBuilderClassNames.toArray(new String[0]))
                        .build());

        RunTimeConfigurationGenerator.GenerateOperation
                .builder()
                .setBuildTimeReadResult(configItem.getReadResult())
                .setClassOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .setLaunchMode(launchModeBuildItem.getLaunchMode())
                .setLiveReloadPossible(launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT
                        || launchModeBuildItem.isAuxiliaryApplication())
                .setAdditionalTypes(typeItems.stream().map(ConfigurationTypeBuildItem::getValueType).collect(toList()))
                .setStaticConfigSources(staticSafeServices(discoveredConfigSources))
                .setStaticConfigSourceProviders(staticConfigSourceProviders)
                .setStaticConfigSourceFactories(staticConfigSourceFactories)
                .setStaticConfigMappings(staticMappings)
                .setStaticConfigBuilders(staticInitConfigBuilders.stream()
                        .map(StaticInitConfigBuilderBuildItem::getBuilderClassName).collect(toSet()))
                .setRuntimeConfigSources(discoveredConfigSources)
                .setRuntimeConfigSourceProviders(discoveredConfigSourceProviders)
                .setRuntimeConfigSourceFactories(discoveredConfigSourceFactories)
                .setRuntimeConfigMappings(runtimeMappings)
                .setRuntimeConfigBuilders(runtimeConfigBuilderClassNames)
                .build()
                .run();
    }

    private static void reportUnknownBuildProperties(LaunchMode launchMode, Set<String> unknownBuildProperties) {
        // So it only reports during the build, because it is very likely that the property is available in runtime
        // and, it will be caught by the RuntimeConfig and log double warnings
        if (!launchMode.isDevOrTest()) {
            ConfigDiagnostic.unknownProperties(unknownBuildProperties);
        }
    }

    @BuildStep
    public void suppressNonRuntimeConfigChanged(
            BuildProducer<SuppressNonRuntimeConfigChangedWarningBuildItem> suppressNonRuntimeConfigChanged) {
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.profile"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.uuid"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.default-locale"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.locales"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.test.arg-line"));
    }

    /**
     * Warns if build time config properties have been changed at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void checkForBuildTimeConfigChange(
            RecorderContext recorderContext,
            ConfigRecorder recorder,
            ConfigurationBuildItem configItem,
            List<SuppressNonRuntimeConfigChangedWarningBuildItem> suppressNonRuntimeConfigChangedWarningItems) {

        recorderContext.registerSubstitution(io.smallrye.config.ConfigValue.class, QuarkusConfigValue.class,
                QuarkusConfigValue.Substitution.class);

        BuildTimeConfigurationReader.ReadResult readResult = configItem.getReadResult();
        Config config = ConfigProvider.getConfig();

        Set<String> excludedConfigKeys = new HashSet<>(suppressNonRuntimeConfigChangedWarningItems.size());
        for (SuppressNonRuntimeConfigChangedWarningBuildItem item : suppressNonRuntimeConfigChangedWarningItems) {
            excludedConfigKeys.add(item.getConfigKey());
        }

        Map<String, ConfigValue> values = new HashMap<>();

        for (final Map.Entry<String, String> entry : readResult.getAllBuildTimeValues().entrySet()) {
            if (excludedConfigKeys.contains(entry.getKey())) {
                continue;
            }
            values.putIfAbsent(entry.getKey(), config.getConfigValue(entry.getKey()));
        }

        for (Map.Entry<String, String> entry : readResult.getBuildTimeRunTimeValues().entrySet()) {
            if (excludedConfigKeys.contains(entry.getKey())) {
                continue;
            }
            values.put(entry.getKey(), config.getConfigValue(entry.getKey()));
        }

        recorder.handleConfigChange(values);
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

    @BuildStep
    public void watchConfigFiles(BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) {
        List<String> configWatchedFiles = new ArrayList<>();

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        String userDir = System.getProperty("user.dir");

        // Main files
        configWatchedFiles.add("application.properties");
        configWatchedFiles.add("META-INF/microprofile-config.properties");
        configWatchedFiles.add(Paths.get(userDir, ".env").toAbsolutePath().toString());
        configWatchedFiles.add(Paths.get(userDir, "config", "application.properties").toAbsolutePath().toString());

        // Profiles
        for (String profile : config.getProfiles()) {
            configWatchedFiles.add(String.format("application-%s.properties", profile));
            configWatchedFiles.add(String.format("META-INF/microprofile-config-%s.properties", profile));
            configWatchedFiles.add(Paths.get(userDir, String.format(".env-%s", profile)).toAbsolutePath().toString());
            configWatchedFiles.add(Paths.get(userDir, "config", String.format("application-%s.properties", profile))
                    .toAbsolutePath().toString());
        }

        Optional<List<URI>> optionalLocations = config.getOptionalValues(SMALLRYE_CONFIG_LOCATIONS, URI.class);
        optionalLocations.ifPresent(locations -> {
            for (URI location : locations) {
                Path path = location.getScheme() != null && location.getScheme().equals("file") ? Paths.get(location)
                        : Paths.get(location.getPath());
                if (Files.isRegularFile(path)) {
                    configWatchedFiles.add(path.toAbsolutePath().toString());
                    for (String profile : config.getProfiles()) {
                        configWatchedFiles.add(appendProfileToFilename(path.toAbsolutePath(), profile));
                    }
                } else if (Files.isDirectory(path)) {
                    try (DirectoryStream<Path> files = Files.newDirectoryStream(path, Files::isRegularFile)) {
                        for (Path file : files) {
                            configWatchedFiles.add(file.toAbsolutePath().toString());
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        });

        for (String configWatchedFile : configWatchedFiles) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(configWatchedFile));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void warnDifferentProfileUsedBetweenBuildAndRunTime(ConfigRecorder configRecorder) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        configRecorder.handleNativeProfileChange(config.getProfiles());
    }

    private String appendProfileToFilename(Path path, String activeProfile) {
        String pathWithoutExtension = getPathWithoutExtension(path);
        return String.format("%s-%s.%s", pathWithoutExtension, activeProfile, getFileExtension(path));
    }

    private static String getFileExtension(Path path) {
        Objects.requireNonNull(path, "path should not be null");
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private static String getPathWithoutExtension(Path path) {
        Objects.requireNonNull(path, "path should not be null");
        String fileName = path.toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private static void generateDefaultsConfigSource(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Map<String, String> defaults,
            String className,
            String sourceName,
            int sourceOrdinal) {

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className)
                .superClass(DefaultsConfigSource.class)
                .setFinal(true)
                .build()) {

            FieldDescriptor properties = FieldDescriptor.of(classCreator.getClassName(), "properties", Map.class);
            classCreator.getFieldCreator(properties).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(Opcodes.ACC_STATIC);
            clinit.writeStaticField(properties, clinit.newInstance(MethodDescriptor.ofConstructor(HashMap.class)));

            ResultHandle map = clinit.readStaticField(properties);
            MethodDescriptor put = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class);
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                clinit.invokeInterfaceMethod(put, map, clinit.load(entry.getKey()), clinit.load(entry.getValue()));
            }
            clinit.returnVoid();

            MethodCreator ctor = classCreator.getMethodCreator("<init>", void.class);
            MethodDescriptor superCtor = MethodDescriptor.ofConstructor(DefaultsConfigSource.class, Map.class, String.class,
                    int.class);
            ctor.invokeSpecialMethod(superCtor, ctor.getThis(), ctor.readStaticField(properties),
                    ctor.load(sourceName), ctor.load(sourceOrdinal));
            ctor.returnVoid();
        }

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(className).build());
    }

    private static void generateMappingsConfigBuilder(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String className,
            Set<ConfigClassWithPrefix> mappings) {

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className)
                .interfaces(ConfigBuilder.class)
                .superClass(MappingsConfigBuilder.class)
                .setFinal(true)
                .build()) {

            MethodCreator method = classCreator.getMethodCreator(CONFIG_BUILDER);
            ResultHandle configBuilder = method.getMethodParam(0);

            MethodDescriptor addMapping = MethodDescriptor.ofMethod(MappingsConfigBuilder.class, "addMapping", void.class,
                    SmallRyeConfigBuilder.class, String.class, String.class);

            for (ConfigClassWithPrefix mapping : mappings) {
                method.invokeStaticMethod(addMapping, configBuilder, method.load(mapping.getKlass().getName()),
                        method.load(mapping.getPrefix()));
            }

            method.returnValue(configBuilder);
        }

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).build());
    }

    private static Set<String> discoverService(
            Class<?> serviceClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> services = new HashSet<>();
        for (String service : classNamesNamedIn(classLoader, SERVICES_PREFIX + serviceClass.getName())) {
            services.add(service);
            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(service).build());
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

    private static Set<ConfigClassWithPrefix> staticSafeConfigMappings(List<ConfigMappingBuildItem> configMappings) {
        return configMappings.stream()
                .filter(ConfigMappingBuildItem::isStaticInitSafe)
                .map(configMapping -> configClassWithPrefix(configMapping.getConfigClass(), configMapping.getPrefix()))
                .collect(toSet());
    }

    private static Set<ConfigClassWithPrefix> runtimeConfigMappings(List<ConfigMappingBuildItem> configMappings) {
        return configMappings.stream()
                .map(configMapping -> configClassWithPrefix(configMapping.getConfigClass(), configMapping.getPrefix()))
                .collect(toSet());
    }
}
