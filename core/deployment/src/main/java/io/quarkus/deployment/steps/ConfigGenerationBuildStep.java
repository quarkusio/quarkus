package io.quarkus.deployment.steps;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.processConfigMapping;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.processExtensionConfigMapping;
import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.CONFIG_RUNTIME_NAME;
import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.CONFIG_STATIC_NAME;
import static io.quarkus.deployment.steps.ConfigBuildSteps.SERVICES_PREFIX;
import static io.quarkus.deployment.util.ServiceUtil.classNamesNamedIn;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.Closeable;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingConfig;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingWriter;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.validator.spi.AdditionalConstrainedClassBuildItem;
import io.quarkus.paths.PathCollection;
import io.quarkus.runtime.BuildAnalyticsConfig;
import io.quarkus.runtime.BuilderConfig;
import io.quarkus.runtime.CommandLineRuntimeConfig;
import io.quarkus.runtime.DebugRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.runtime.configuration.AbstractConfigBuilder;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.quarkus.runtime.configuration.ConfigDiagnostic;
import io.quarkus.runtime.configuration.ConfigRecorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.DisableableConfigSource;
import io.quarkus.runtime.configuration.QuarkusConfigValue;
import io.quarkus.runtime.configuration.RuntimeConfigBuilder;
import io.quarkus.runtime.configuration.RuntimeOverrideConfigSource;
import io.quarkus.runtime.configuration.RuntimeOverrideConfigSourceBuilder;
import io.quarkus.runtime.configuration.StaticInitConfigBuilder;
import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class ConfigGenerationBuildStep {
    private static final MethodDescriptor CONFIG_BUILDER = MethodDescriptor.ofMethod(ConfigBuilder.class,
            "configBuilder", SmallRyeConfigBuilder.class, SmallRyeConfigBuilder.class);
    private static final MethodDescriptor WITH_SOURCES = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withSources", SmallRyeConfigBuilder.class, ConfigSource[].class);

    @BuildStep
    void nativeSupport(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClassProducer) {
        runtimeInitializedClassProducer.produce(new RuntimeInitializedClassBuildItem(
                "io.quarkus.runtime.configuration.RuntimeConfigBuilder$UuidConfigSource$Holder"));
    }

    @BuildStep
    void buildTimeRunTimeConfig(
            ConfigurationBuildItem configItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        String builderClassName = "io.quarkus.runtime.generated.BuildTimeRunTimeFixedConfigSourceBuilder";
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(builderClassName)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            FieldDescriptor source = FieldDescriptor.of(classCreator.getClassName(), "source", ConfigSource.class);
            classCreator.getFieldCreator(source).setModifiers(ACC_STATIC | Opcodes.ACC_FINAL);

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(ACC_STATIC);

            ResultHandle map = clinit.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            MethodDescriptor put = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class);
            for (Map.Entry<String, ConfigValue> entry : configItem.getReadResult().getBuildTimeRunTimeValues().entrySet()) {
                if (entry.getValue().getValue() != null) {
                    clinit.invokeInterfaceMethod(put, map, clinit.load(entry.getKey()),
                            clinit.load(entry.getValue().getValue()));
                }
            }

            ResultHandle defaultValuesSource = clinit.newInstance(
                    MethodDescriptor.ofConstructor(DefaultValuesConfigSource.class, Map.class, String.class, int.class), map,
                    clinit.load("BuildTime RunTime Fixed"), clinit.load(Integer.MAX_VALUE));

            ResultHandle disableableConfigSource = clinit.newInstance(
                    MethodDescriptor.ofConstructor(DisableableConfigSource.class, ConfigSource.class),
                    defaultValuesSource);
            clinit.writeStaticField(source, disableableConfigSource);
            clinit.returnVoid();

            MethodCreator method = classCreator.getMethodCreator(CONFIG_BUILDER);
            ResultHandle configBuilder = method.getMethodParam(0);

            ResultHandle configSources = method.newArray(ConfigSource.class, 1);
            method.writeArrayValue(configSources, 0, method.readStaticField(source));

            method.invokeVirtualMethod(WITH_SOURCES, configBuilder, configSources);

            method.returnValue(configBuilder);
        }

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(builderClassName).reason(getClass().getName()).build());
        staticInitConfigBuilder.produce(new StaticInitConfigBuilderBuildItem(builderClassName));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(builderClassName));
    }

    @BuildStep(onlyIfNot = { IsProduction.class }) // for dev or test
    void runtimeOverrideConfig(
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        staticInitConfigBuilder
                .produce(new StaticInitConfigBuilderBuildItem(RuntimeOverrideConfigSourceBuilder.class.getName()));
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(RuntimeOverrideConfigSourceBuilder.class.getName()));
    }

    @BuildStep
    void generateMappings(
            NativeConfig nativeConfig,
            ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigClassBuildItem> configClasses,
            BuildProducer<AdditionalConstrainedClassBuildItem> additionalConstrainedClasses) {

        Map<String, GeneratedClassBuildItem> generatedConfigClasses = new HashMap<>();

        processConfigMapping(nativeConfig, configItem, combinedIndex, generatedConfigClasses, reflectiveClasses,
                reflectiveMethods,
                configClasses, additionalConstrainedClasses);

        List<ConfigClass> buildTimeRunTimeMappings = configItem.getReadResult().getBuildTimeRunTimeMappings();
        for (ConfigClass buildTimeRunTimeMapping : buildTimeRunTimeMappings) {
            processExtensionConfigMapping(nativeConfig, buildTimeRunTimeMapping, combinedIndex, generatedConfigClasses,
                    reflectiveClasses,
                    reflectiveMethods, configClasses, additionalConstrainedClasses);
        }

        List<ConfigClass> runTimeMappings = configItem.getReadResult().getRunTimeMappings();
        for (ConfigClass runTimeMapping : runTimeMappings) {
            processExtensionConfigMapping(nativeConfig, runTimeMapping, combinedIndex, generatedConfigClasses,
                    reflectiveClasses,
                    reflectiveMethods,
                    configClasses, additionalConstrainedClasses);
        }

        for (GeneratedClassBuildItem generatedConfigClass : generatedConfigClasses.values()) {
            generatedClasses.produce(generatedConfigClass);
        }
    }

    @BuildStep
    void generateBuilders(
            ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            List<ConfigMappingBuildItem> configMappings,
            List<RunTimeConfigurationDefaultBuildItem> runTimeDefaults,
            List<StaticInitConfigBuilderBuildItem> staticInitConfigBuilders,
            List<RunTimeConfigBuilderBuildItem> runTimeConfigBuilders,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {

        Map<String, String> defaultValues = new HashMap<>();
        for (RunTimeConfigurationDefaultBuildItem e : runTimeDefaults) {
            defaultValues.put(e.getKey(), e.getValue());
        }

        Set<String> converters = discoverService(Converter.class, reflectiveClass);
        Set<String> interceptors = discoverService(ConfigSourceInterceptor.class, reflectiveClass);
        Set<String> interceptorFactories = discoverService(ConfigSourceInterceptorFactory.class, reflectiveClass);
        Set<String> configSources = discoverService(ConfigSource.class, reflectiveClass);
        Set<String> configSourceProviders = discoverService(ConfigSourceProvider.class, reflectiveClass);
        Set<String> configSourceFactories = discoverService(ConfigSourceFactory.class, reflectiveClass);
        Set<String> secretKeyHandlers = discoverService(SecretKeysHandler.class, reflectiveClass);
        Set<String> secretKeyHandlerFactories = discoverService(SecretKeysHandlerFactory.class, reflectiveClass);
        Set<String> configCustomizers = discoverService(SmallRyeConfigBuilderCustomizer.class, reflectiveClass);

        // TODO - introduce a way to ignore mappings that are only used for documentation or to prevent warnings
        Set<ConfigClass> ignoreMappings = new LinkedHashSet<>();
        ignoreMappings.add(ConfigClass.configClass(BuildAnalyticsConfig.class, "quarkus.analytics"));
        ignoreMappings.add(ConfigClass.configClass(BuilderConfig.class, "quarkus.builder"));
        ignoreMappings.add(ConfigClass.configClass(CommandLineRuntimeConfig.class, "quarkus"));
        ignoreMappings.add(ConfigClass.configClass(DebugRuntimeConfig.class, "quarkus.debug"));

        Set<ConfigClass> allMappings = new LinkedHashSet<>();
        allMappings.addAll(staticSafeConfigMappings(configMappings));
        allMappings.addAll(runtimeConfigMappings(configMappings));
        allMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());
        allMappings.addAll(configItem.getReadResult().getRunTimeMappings());
        allMappings.removeAll(ignoreMappings);

        Set<ConfigClass> buildTimeRuntimeMappings = new LinkedHashSet<>(
                configItem.getReadResult().getBuildTimeRunTimeMappings());
        buildTimeRuntimeMappings.removeAll(ignoreMappings);

        // Shared components
        Map<Object, FieldDescriptor> sharedFields = generateSharedConfig(
                generatedClass,
                combinedIndex,
                converters, allMappings, buildTimeRuntimeMappings);

        // For Static Init Config
        Set<ConfigClass> staticMappings = new LinkedHashSet<>(staticSafeConfigMappings(configMappings));
        Set<String> staticCustomizers = new LinkedHashSet<>(staticSafeServices(configCustomizers));
        staticCustomizers.add(StaticInitConfigBuilder.class.getName());

        generateConfigBuilder(generatedClass, reflectiveClass, CONFIG_STATIC_NAME,
                combinedIndex,
                sharedFields,
                defaultValues,
                Map.of(),
                converters,
                interceptors,
                staticSafeServices(interceptorFactories),
                staticSafeServices(configSources),
                staticSafeServices(configSourceProviders),
                staticSafeServices(configSourceFactories),
                secretKeyHandlers,
                staticSafeServices(secretKeyHandlerFactories),
                buildTimeRuntimeMappings,
                Set.of(),
                staticMappings,
                configItem.getReadResult().getMappingsIgnorePaths(),
                staticCustomizers,
                staticInitConfigBuilders.stream().map(StaticInitConfigBuilderBuildItem::getBuilderClassName).collect(toSet()));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CONFIG_STATIC_NAME).build());

        // For RunTime Config
        Map<String, String> runtimeValues = new HashMap<>();
        for (Entry<String, ConfigValue> entry : configItem.getReadResult().getRunTimeValues().entrySet()) {
            runtimeValues.put(entry.getKey(), entry.getValue().getRawValue());
        }
        Set<ConfigClass> runTimeMappings = new LinkedHashSet<>();
        runTimeMappings.addAll(runtimeConfigMappings(configMappings));
        runTimeMappings.addAll(configItem.getReadResult().getBuildTimeRunTimeMappings());
        runTimeMappings.addAll(configItem.getReadResult().getRunTimeMappings());
        runTimeMappings.removeAll(ignoreMappings);
        Set<String> runtimeCustomizers = new LinkedHashSet<>(configCustomizers);
        runtimeCustomizers.add(RuntimeConfigBuilder.class.getName());

        generateConfigBuilder(generatedClass, reflectiveClass, CONFIG_RUNTIME_NAME,
                combinedIndex,
                sharedFields,
                defaultValues,
                runtimeValues,
                converters,
                interceptors,
                interceptorFactories,
                configSources,
                configSourceProviders,
                configSourceFactories,
                secretKeyHandlers,
                secretKeyHandlerFactories,
                buildTimeRuntimeMappings,
                staticMappings,
                runTimeMappings,
                configItem.getReadResult().getMappingsIgnorePaths(),
                runtimeCustomizers,
                runTimeConfigBuilders.stream().map(RunTimeConfigBuilderBuildItem::getBuilderClassName).collect(toSet()));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CONFIG_RUNTIME_NAME).build());
    }

    /**
     * Generate the Config class that instantiates MP Config and holds all the config objects
     */
    @BuildStep
    void generateConfigClass(
            ConfigurationBuildItem configItem,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem) {

        // So it only reports during the build, because it is very likely that the property is available in runtime
        // and, it will be caught by the RuntimeConfig and log double warnings
        if (!launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            ConfigDiagnostic.unknownProperties(configItem.getReadResult().getUnknownBuildProperties());
        }

        // TODO - Test live reload with ConfigSource
        if (liveReloadBuildItem.isLiveReload()) {
            return;
        }

        RunTimeConfigurationGenerator.GenerateOperation
                .builder()
                .setLaunchMode(launchModeBuildItem.getLaunchMode())
                .setBuildTimeReadResult(configItem.getReadResult())
                .setClassOutput(new GeneratedClassGizmoAdaptor(generatedClass, false))
                .setLiveReloadPossible(launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT
                        || launchModeBuildItem.isAuxiliaryApplication())
                .build()
                .run();
    }

    @BuildStep
    public void suppressNonRuntimeConfigChanged(
            BuildProducer<SuppressNonRuntimeConfigChangedWarningBuildItem> suppressNonRuntimeConfigChanged) {
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.profile"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.default-locale"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.locales"));
        suppressNonRuntimeConfigChanged.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.test.arg-line"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void releaseConfigOnShutdown(ShutdownContextBuildItem shutdownContext,
            ConfigRecorder recorder) {
        recorder.releaseConfig(shutdownContext);
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

        Set<String> excludedConfigKeys = new HashSet<>(suppressNonRuntimeConfigChangedWarningItems.size());
        for (SuppressNonRuntimeConfigChangedWarningBuildItem item : suppressNonRuntimeConfigChangedWarningItems) {
            excludedConfigKeys.add(item.getConfigKey());
        }

        List<ConfigValue> values = new ArrayList<>();
        for (Map.Entry<String, ConfigValue> entry : configItem.getReadResult().getBuildTimeRunTimeValues().entrySet()) {
            if (excludedConfigKeys.contains(entry.getKey())) {
                continue;
            }

            ConfigValue value = entry.getValue();
            values.add(ConfigValue.builder()
                    .withName(value.getName())
                    .withValue(value.getValue())
                    .withRawValue(value.getRawValue())
                    .withConfigSourceOrdinal(value.getConfigSourceOrdinal())
                    .build());
        }

        recorder.handleConfigChange(values);
    }

    @BuildStep(onlyIfNot = { IsProduction.class })
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

    @BuildStep
    @Record(RUNTIME_INIT)
    void reportDeprecatedMappingProperties(ConfigRecorder configRecorder, ConfigurationBuildItem configBuildItem) {
        // Build Time
        List<ConfigClass> visibleBuildTimeMappings = new ArrayList<>();
        visibleBuildTimeMappings.addAll(configBuildItem.getReadResult().getBuildTimeMappings());
        visibleBuildTimeMappings.addAll(configBuildItem.getReadResult().getBuildTimeRunTimeMappings());
        Map<String, String> deprecatedProperties = deprecatedProperties(visibleBuildTimeMappings);
        ConfigDiagnostic.deprecatedProperties(deprecatedProperties);

        // Runtime
        Map<String, String> runtimeDeprecatedProperties = deprecatedProperties(
                configBuildItem.getReadResult().getRunTimeMappings());
        configRecorder.deprecatedProperties(runtimeDeprecatedProperties);
    }

    private static Map<String, String> deprecatedProperties(List<ConfigClass> configClasses) {
        Map<String, String> deprecatedProperties = new HashMap<>();
        for (ConfigClass buildTimeMapping : configClasses) {
            Map<String, ConfigMappingInterface.Property> properties = ConfigMappings.getProperties(buildTimeMapping);
            for (Map.Entry<String, ConfigMappingInterface.Property> entry : properties.entrySet()) {
                Deprecated deprecated = entry.getValue().getMethod().getAnnotation(Deprecated.class);
                if (deprecated != null) {
                    // TODO - add javadoc message
                    deprecatedProperties.put(entry.getKey(), null);
                }
            }
        }
        return deprecatedProperties;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void unknownConfigFiles(
            ApplicationArchivesBuildItem applicationArchives,
            LaunchModeBuildItem launchModeBuildItem,
            ConfigRecorder configRecorder) throws Exception {

        Set<Path> buildTimeFiles = new HashSet<>();
        PathCollection rootDirectories = applicationArchives.getRootArchive().getRootDirectories();
        for (Path directory : rootDirectories) {
            buildTimeFiles.addAll(ConfigDiagnostic.configFiles(directory));
        }
        buildTimeFiles.addAll(ConfigDiagnostic.configFilesFromLocations());

        // Report always at build time since config folder and locations may differ from build to runtime
        ConfigDiagnostic.unknownConfigFiles(buildTimeFiles);

        // No need to include the application files, because they don't change
        if (!launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            configRecorder.unknownConfigFiles();
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void warnDifferentProfileUsedBetweenBuildAndRunTime(ConfigRecorder configRecorder) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        configRecorder.handleNativeProfileChange(config.getProfiles());
    }

    @BuildStep(onlyIf = IsProduction.class)
    void persistReadConfigOptions(
            BuildProducer<ArtifactResultBuildItem> dummy,
            QuarkusBuildCloseablesBuildItem closeables,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            ConfigurationBuildItem configBuildItem,
            ConfigTrackingConfig configTrackingConfig) {
        var readOptionsProvider = configBuildItem.getReadResult().getReadOptionsProvider();
        if (readOptionsProvider != null) {
            closeables.add(new Closeable() {
                @Override
                public void close() throws IOException {
                    ConfigTrackingWriter.write(
                            readOptionsProvider.getReadOptions(),
                            configTrackingConfig,
                            configBuildItem.getReadResult(),
                            ConfigUtils.getProfiles(),
                            buildSystemTargetBuildItem.getOutputDirectory());
                }
            });
        }
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

    private static final MethodDescriptor NEW_BUILDER = MethodDescriptor.ofConstructor(SmallRyeConfigBuilder.class);
    private static final MethodDescriptor BUILD = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "build", SmallRyeConfig.class);
    private static final MethodDescriptor GET_CONFIG_MAPPING = MethodDescriptor.ofMethod(SmallRyeConfig.class,
            "getConfigMapping", Object.class, Class.class, String.class);
    private static final MethodDescriptor BUILDER_CUSTOMIZER = MethodDescriptor.ofMethod(SmallRyeConfigBuilderCustomizer.class,
            "configBuilder",
            void.class, SmallRyeConfigBuilder.class);
    private static final MethodDescriptor WITH_DEFAULTS = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withDefaultValues",
            void.class, SmallRyeConfigBuilder.class, Map.class);
    private static final MethodDescriptor WITH_RUNTIME_VALUES = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withRuntimeValues",
            void.class, SmallRyeConfigBuilder.class, Map.class);
    private static final MethodDescriptor WITH_CONVERTER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withConverter",
            void.class, SmallRyeConfigBuilder.class, String.class, int.class, Converter.class);
    private static final MethodDescriptor WITH_INTERCEPTOR = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withInterceptor",
            void.class, SmallRyeConfigBuilder.class, ConfigSourceInterceptor.class);
    private static final MethodDescriptor WITH_INTERCEPTOR_FACTORY = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withInterceptorFactory",
            void.class, SmallRyeConfigBuilder.class, ConfigSourceInterceptorFactory.class);
    private static final MethodDescriptor WITH_SOURCE = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withSource",
            void.class, SmallRyeConfigBuilder.class, ConfigSource.class);
    private static final MethodDescriptor WITH_SOURCE_PROVIDER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withSource",
            void.class, SmallRyeConfigBuilder.class, ConfigSourceProvider.class);
    private static final MethodDescriptor WITH_SOURCE_FACTORY = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withSource",
            void.class, SmallRyeConfigBuilder.class, ConfigSourceFactory.class);
    private static final MethodDescriptor WITH_SECRET_HANDLER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withSecretKeyHandler",
            void.class, SmallRyeConfigBuilder.class, SecretKeysHandler.class);
    private static final MethodDescriptor WITH_SECRET_HANDLER_FACTORY = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withSecretKeyHandler",
            void.class, SmallRyeConfigBuilder.class, SecretKeysHandlerFactory.class);
    private static final MethodDescriptor WITH_MAPPING = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withMapping",
            void.class, SmallRyeConfigBuilder.class, ConfigClass.class);
    private static final MethodDescriptor WITH_MAPPING_INSTANCE = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withMappingInstance",
            void.class, SmallRyeConfigBuilder.class, ConfigClass.class, Object.class);
    private static final MethodDescriptor WITH_MAPPING_INSTANCE_FROM_CONFIG = MethodDescriptor.ofMethod(
            AbstractConfigBuilder.class,
            "withMappingInstance",
            void.class, SmallRyeConfigBuilder.class, ConfigClass.class);
    private static final MethodDescriptor WITH_MAPPING_IGNORE = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withMappingIgnore",
            void.class, SmallRyeConfigBuilder.class, String.class);
    private static final MethodDescriptor WITH_CUSTOMIZER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withCustomizer",
            void.class, SmallRyeConfigBuilder.class, SmallRyeConfigBuilderCustomizer.class);
    private static final MethodDescriptor WITH_BUILDER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withBuilder",
            void.class, SmallRyeConfigBuilder.class, ConfigBuilder.class);
    private static final MethodDescriptor CONFIG_CLASS = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "configClass",
            ConfigClass.class, String.class, String.class);
    private static final MethodDescriptor ENSURE_LOADED = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "ensureLoaded",
            void.class, String.class);
    private static final MethodDescriptor MAP_NEW = MethodDescriptor.ofConstructor(HashMap.class, int.class);
    private static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(HashMap.class,
            "put",
            Object.class, Object.class, Object.class);

    private static final DotName CONVERTER_NAME = DotName.createSimple(Converter.class.getName());
    private static final DotName PRIORITY_NAME = DotName.createSimple(Priority.class.getName());

    private static Map<Object, FieldDescriptor> generateSharedConfig(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            CombinedIndexBuildItem combinedIndex,
            Set<String> converters,
            Set<ConfigClass> mappings,
            Set<ConfigClass> staticMappings) {

        Map<Object, FieldDescriptor> fields = new HashMap<>();
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className("io.quarkus.runtime.generated.SharedConfig")
                .superClass(AbstractConfigBuilder.class)
                .setFinal(true)
                .build()) {

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(ACC_STATIC);

            int converterIndex = 0;
            for (String converter : converters) {
                String fieldName = "conv$" + converterIndex++;
                FieldDescriptor converterField = classCreator.getFieldCreator(fieldName, Converter.class)
                        .setModifiers(ACC_STATIC).getFieldDescriptor();
                clinit.writeStaticField(converterField, clinit.newInstance(MethodDescriptor.ofConstructor(converter)));
                fields.put(converter, converterField);
            }

            int configClassIndex = 0;
            for (ConfigClass mapping : mappings) {
                FieldDescriptor configClassField = classCreator
                        .getFieldCreator("configClass$" + configClassIndex++, ConfigClass.class)
                        .setModifiers(ACC_STATIC).getFieldDescriptor();
                clinit.writeStaticField(configClassField, clinit.invokeStaticMethod(CONFIG_CLASS,
                        clinit.load(mapping.getType().getName()), clinit.load(mapping.getPrefix())));

                // Cache implementation types of nested elements
                List<ConfigMappingMetadata> configMappingsMetadata = ConfigMappingLoader
                        .getConfigMappingsMetadata(mapping.getType());
                for (ConfigMappingMetadata configMappingMetadata : configMappingsMetadata) {
                    clinit.invokeStaticMethod(ENSURE_LOADED, clinit.load(configMappingMetadata.getInterfaceType().getName()));
                }

                fields.put(mapping, configClassField);
            }

            // init build and runtime fixed mappings
            ResultHandle configBuilder = clinit.newInstance(NEW_BUILDER);
            clinit.invokeStaticMethod(MethodDescriptor.ofMethod(AbstractConfigBuilder.class, "withSharedBuilder", void.class,
                    SmallRyeConfigBuilder.class), configBuilder);
            for (String converter : converters) {
                ClassInfo converterClass = combinedIndex.getComputingIndex().getClassByName(converter);
                Type type = getConverterType(converterClass, combinedIndex);
                AnnotationInstance priorityAnnotation = converterClass.annotation(PRIORITY_NAME);
                int priority = priorityAnnotation != null ? priorityAnnotation.value().asInt() : 100;

                clinit.invokeStaticMethod(WITH_CONVERTER, configBuilder,
                        clinit.load(type.name().toString()),
                        clinit.load(priority),
                        clinit.readStaticField(fields.get(converter)));
            }
            for (ConfigClass mapping : staticMappings) {
                clinit.invokeStaticMethod(WITH_MAPPING, configBuilder, clinit.readStaticField(fields.get(mapping)));
            }
            clinit.invokeStaticMethod(WITH_BUILDER, configBuilder, clinit.newInstance(
                    MethodDescriptor.ofConstructor("io.quarkus.runtime.generated.BuildTimeRunTimeFixedConfigSourceBuilder")));
            ResultHandle config = clinit.invokeVirtualMethod(BUILD, configBuilder);
            int mappingIndex = 0;
            for (ConfigClass mapping : staticMappings) {
                FieldDescriptor mappingField = classCreator.getFieldCreator("mapping$" + mappingIndex++, mapping.getType())
                        .setModifiers(ACC_STATIC).getFieldDescriptor();
                clinit.writeStaticField(mappingField, clinit.invokeVirtualMethod(GET_CONFIG_MAPPING, config,
                        clinit.loadClass(mapping.getType()), clinit.load(mapping.getPrefix())));
                fields.put(fields.get(mapping), mappingField);
            }

            clinit.returnVoid();
        }

        return fields;
    }

    private static void generateConfigBuilder(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            String className,
            CombinedIndexBuildItem combinedIndex,
            Map<Object, FieldDescriptor> sharedFields,
            Map<String, String> defaultValues,
            Map<String, String> runtimeValues,
            Set<String> converters,
            Set<String> interceptors,
            Set<String> interceptorFactories,
            Set<String> configSources,
            Set<String> configSourceProviders,
            Set<String> configSourceFactories,
            Set<String> secretKeyHandlers,
            Set<String> secretKeyHandlerFactories,
            Set<ConfigClass> mappingsShared,
            Set<ConfigClass> mappingsInstances,
            Set<ConfigClass> mappings,
            Set<String> mappingsIgnorePaths,
            Set<String> configCustomizers,
            Set<String> configBuilders) {

        // First generate a customizer with all components to ensure order
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className + "Customizer")
                .superClass(AbstractConfigBuilder.class)
                .interfaces(SmallRyeConfigBuilderCustomizer.class)
                .setFinal(true)
                .build()) {

            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(ACC_STATIC);

            MethodCreator method = classCreator.getMethodCreator(BUILDER_CUSTOMIZER);
            ResultHandle configBuilder = method.getMethodParam(0);

            FieldDescriptor defaultsField = classCreator.getFieldCreator("defaults", Map.class).setModifiers(ACC_STATIC)
                    .getFieldDescriptor();
            clinit.writeStaticField(defaultsField,
                    clinit.newInstance(MAP_NEW, clinit.load((int) ((float) defaultValues.size() / 0.75f + 1.0f))));
            for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
                clinit.invokeVirtualMethod(MAP_PUT, clinit.readStaticField(defaultsField), clinit.load(entry.getKey()),
                        clinit.load(entry.getValue()));
            }
            method.invokeStaticMethod(WITH_DEFAULTS, configBuilder, method.readStaticField(defaultsField));

            FieldDescriptor runtimeValuesField = classCreator.getFieldCreator("runtimeValues", Map.class)
                    .setModifiers(ACC_STATIC).getFieldDescriptor();
            clinit.writeStaticField(runtimeValuesField,
                    clinit.newInstance(MAP_NEW, clinit.load((int) ((float) runtimeValues.size() / 0.75f + 1.0f))));
            for (Map.Entry<String, String> entry : runtimeValues.entrySet()) {
                clinit.invokeVirtualMethod(MAP_PUT, clinit.readStaticField(runtimeValuesField), clinit.load(entry.getKey()),
                        clinit.load(entry.getValue()));
            }
            method.invokeStaticMethod(WITH_RUNTIME_VALUES, configBuilder, method.readStaticField(runtimeValuesField));

            for (String converter : converters) {
                ClassInfo converterClass = combinedIndex.getComputingIndex().getClassByName(converter);
                Type type = getConverterType(converterClass, combinedIndex);
                AnnotationInstance priorityAnnotation = converterClass.annotation(PRIORITY_NAME);
                int priority = priorityAnnotation != null ? priorityAnnotation.value().asInt() : 100;

                method.invokeStaticMethod(WITH_CONVERTER, configBuilder,
                        method.load(type.name().toString()),
                        method.load(priority),
                        method.readStaticField(sharedFields.get(converter)));
            }

            for (String interceptor : interceptors) {
                method.invokeStaticMethod(WITH_INTERCEPTOR, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(interceptor)));
            }

            for (String interceptorFactory : interceptorFactories) {
                method.invokeStaticMethod(WITH_INTERCEPTOR_FACTORY, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(interceptorFactory)));
            }

            for (String configSource : configSources) {
                method.invokeStaticMethod(WITH_SOURCE, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(configSource)));
            }

            for (String configSourceProvider : configSourceProviders) {
                method.invokeStaticMethod(WITH_SOURCE_PROVIDER, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(configSourceProvider)));
            }

            for (String configSourceFactory : configSourceFactories) {
                method.invokeStaticMethod(WITH_SOURCE_FACTORY, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(configSourceFactory)));
            }

            for (String secretKeyHandler : secretKeyHandlers) {
                method.invokeStaticMethod(WITH_SECRET_HANDLER, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(secretKeyHandler)));
            }

            for (String secretKeyHandlerFactory : secretKeyHandlerFactories) {
                method.invokeStaticMethod(WITH_SECRET_HANDLER_FACTORY, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(secretKeyHandlerFactory)));
            }

            for (ConfigClass mappingShared : mappingsShared) {
                FieldDescriptor configClassField = sharedFields.get(mappingShared);
                FieldDescriptor mappingInstanceField = sharedFields.get(configClassField);
                method.invokeStaticMethod(WITH_MAPPING_INSTANCE, configBuilder, method.readStaticField(configClassField),
                        method.readStaticField(mappingInstanceField));
            }

            for (ConfigClass mappingInstance : mappingsInstances) {
                FieldDescriptor configClassField = sharedFields.get(mappingInstance);
                method.invokeStaticMethod(WITH_MAPPING_INSTANCE_FROM_CONFIG, configBuilder,
                        method.readStaticField(configClassField));
            }

            mappings.removeAll(mappingsInstances);
            for (ConfigClass mapping : mappings) {
                method.invokeStaticMethod(WITH_MAPPING, configBuilder, method.readStaticField(sharedFields.get(mapping)));
            }

            for (String path : mappingsIgnorePaths) {
                method.invokeStaticMethod(WITH_MAPPING_IGNORE, configBuilder, method.load(path));
            }

            clinit.returnVoid();
            method.returnVoid();
        }

        configCustomizers.add(className + "Customizer");

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className)
                .superClass(AbstractConfigBuilder.class)
                .interfaces(SmallRyeConfigBuilderCustomizer.class)
                .setFinal(true)
                .build()) {

            MethodCreator method = classCreator.getMethodCreator(BUILDER_CUSTOMIZER);
            ResultHandle configBuilder = method.getMethodParam(0);

            for (String configCustomizer : configCustomizers) {
                method.invokeStaticMethod(WITH_CUSTOMIZER, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(configCustomizer)));
            }

            for (String builder : configBuilders) {
                method.invokeStaticMethod(WITH_BUILDER, configBuilder,
                        method.newInstance(MethodDescriptor.ofConstructor(builder)));
            }

            method.returnVoid();
        }

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).build());
    }

    private static Set<String> discoverService(
            Class<?> serviceClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> services = new LinkedHashSet<>();
        for (String service : classNamesNamedIn(classLoader, SERVICES_PREFIX + serviceClass.getName())) {
            // The discovery includes deployment modules, so we only include services available at runtime
            if (QuarkusClassLoader.isClassPresentAtRuntime(service)) {
                services.add(service);
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(service).build());
            }
        }
        return services;
    }

    private static Set<String> staticSafeServices(Set<String> services) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Set<String> staticSafe = new LinkedHashSet<>();
        for (String service : services) {
            // SmallRye Config services are always safe, but they cannot be annotated with @StaticInitSafe
            if (service.startsWith("io.smallrye.config.")) {
                staticSafe.add(service);
                continue;
            }

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

    private static Set<ConfigClass> staticSafeConfigMappings(List<ConfigMappingBuildItem> configMappings) {
        return configMappings.stream()
                .filter(ConfigMappingBuildItem::isStaticInitSafe)
                .map(ConfigMappingBuildItem::toConfigClass)
                .collect(toSet());
    }

    private static Set<ConfigClass> runtimeConfigMappings(List<ConfigMappingBuildItem> configMappings) {
        return configMappings.stream()
                .map(ConfigMappingBuildItem::toConfigClass)
                .collect(toSet());
    }

    private static Type getConverterType(final ClassInfo converter, final CombinedIndexBuildItem combinedIndex) {
        if (converter.name().toString().equals(Object.class.getName())) {
            throw new IllegalArgumentException(
                    "Can not add converter " + converter.name() + " that is not parameterized with a type");
        }

        for (Type type : converter.interfaceTypes()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = type.asParameterizedType();
                if (parameterizedType.name().equals(CONVERTER_NAME)) {
                    List<Type> arguments = parameterizedType.arguments();
                    if (arguments.size() != 1) {
                        throw new IllegalArgumentException(
                                "Converter " + converter.name() + " must be parameterized with a single type");
                    }
                    return arguments.get(0);
                }
            }
        }

        return getConverterType(combinedIndex.getComputingIndex().getClassByName(converter.superName()), combinedIndex);
    }
}
