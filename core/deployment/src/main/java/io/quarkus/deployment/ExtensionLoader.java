package io.quarkus.deployment;

import static io.quarkus.deployment.util.ReflectUtil.isBuildProducerOf;
import static io.quarkus.deployment.util.ReflectUtil.isConsumerOf;
import static io.quarkus.deployment.util.ReflectUtil.isListOf;
import static io.quarkus.deployment.util.ReflectUtil.isOptionalOf;
import static io.quarkus.deployment.util.ReflectUtil.isSupplierOf;
import static io.quarkus.deployment.util.ReflectUtil.isSupplierOfOptionalOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeExtends;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeIs;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOfParameter;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.common.function.Functions;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.ConsumeFlag;
import io.quarkus.builder.ConsumeFlags;
import io.quarkus.builder.ProduceFlag;
import io.quarkus.builder.ProduceFlags;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.BuildTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.BuildTimeRunTimeFixedConfigurationBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.UnmatchedConfigBuildItem;
import io.quarkus.deployment.configuration.ConfigDefinition;
import io.quarkus.deployment.configuration.DefaultValuesConfigurationSource;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSource;
import io.quarkus.runtime.configuration.ConverterSupport;
import io.quarkus.runtime.configuration.DeploymentProfileConfigSource;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Utility class to load build steps, runtime recorders, and configuration roots from a given extension class.
 */
public final class ExtensionLoader {
    private ExtensionLoader() {
    }

    private static final Logger cfgLog = Logger.getLogger("io.quarkus.configuration");

    public static final String BUILD_TIME_CONFIG = "io.quarkus.runtime.generated.BuildTimeConfig";
    public static final String BUILD_TIME_CONFIG_ROOT = "io.quarkus.runtime.generated.BuildTimeConfigRoot";
    public static final String RUN_TIME_CONFIG = "io.quarkus.runtime.generated.RunTimeConfig";
    public static final String RUN_TIME_CONFIG_ROOT = "io.quarkus.runtime.generated.RunTimeConfigRoot";

    private static final FieldDescriptor RUN_TIME_CONFIG_FIELD = FieldDescriptor.of(RUN_TIME_CONFIG, "runConfig",
            RUN_TIME_CONFIG_ROOT);
    private static final FieldDescriptor BUILD_TIME_CONFIG_FIELD = FieldDescriptor.of(BUILD_TIME_CONFIG, "buildConfig",
            BUILD_TIME_CONFIG_ROOT);

    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    private static boolean isRecorder(AnnotatedElement element) {
        return element.isAnnotationPresent(Recorder.class) || element.isAnnotationPresent(Template.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, new Properties());
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @param launchMode the launch mode
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, LaunchMode launchMode)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, new Properties(), launchMode);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @param buildSystemProps the build system properties to use
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, Properties buildSystemProps)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, buildSystemProps, LaunchMode.NORMAL);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @param buildSystemProps the build system properties to use
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, Properties buildSystemProps,
            LaunchMode launchMode)
            throws IOException, ClassNotFoundException {

        // set up the configuration definitions
        final ConfigDefinition buildTimeConfig = new ConfigDefinition(FieldDescriptor.of("Bogus", "No field", "Nothing"));
        final ConfigDefinition buildTimeRunTimeConfig = new ConfigDefinition(BUILD_TIME_CONFIG_FIELD);
        final ConfigDefinition runTimeConfig = new ConfigDefinition(RUN_TIME_CONFIG_FIELD, true);

        // populate it with all known types
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, CONFIG_ROOTS_LIST)) {
            final ConfigRoot annotation = clazz.getAnnotation(ConfigRoot.class);
            if (annotation == null) {
                cfgLog.warnf("Ignoring configuration root %s because it has no annotation", clazz);
            } else {
                final ConfigPhase phase = annotation.phase();
                if (phase == ConfigPhase.RUN_TIME) {
                    runTimeConfig.registerConfigRoot(clazz);
                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                    buildTimeRunTimeConfig.registerConfigRoot(clazz);
                } else if (phase == ConfigPhase.BUILD_TIME) {
                    buildTimeConfig.registerConfigRoot(clazz);
                } else {
                    cfgLog.warnf("Unrecognized configuration phase \"%s\" on %s", phase, clazz);
                }
            }
        }

        // now prepare & load the build configuration
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();

        // expand properties
        final ExpandingConfigSource.Cache cache = new ExpandingConfigSource.Cache();
        builder.withWrapper(ExpandingConfigSource.wrapper(cache));
        builder.withWrapper(DeploymentProfileConfigSource.wrapper());
        builder.addDefaultSources();
        final ApplicationPropertiesConfigSource.InJar inJar = new ApplicationPropertiesConfigSource.InJar();
        final DefaultValuesConfigurationSource defaultSource = new DefaultValuesConfigurationSource(
                buildTimeConfig.getLeafPatterns());
        final PropertiesConfigSource pcs = new PropertiesConfigSource(buildSystemProps, "Build system");

        builder.withSources(inJar, defaultSource, pcs);

        // populate builder with all converters loaded from ServiceLoader
        ConverterSupport.populateConverters(builder);

        final SmallRyeConfig src = (SmallRyeConfig) builder
                .addDefaultSources()
                .addDiscoveredSources()
                .addDiscoveredConverters()
                .build();

        SmallRyeConfigProviderResolver.instance().registerConfig(src, classLoader);

        Set<String> unmatched = new HashSet<>();

        ConfigDefinition.loadConfiguration(cache, src,
                unmatched,
                buildTimeConfig,
                buildTimeRunTimeConfig, // this one is only for generating a default-values config source
                runTimeConfig);

        unmatched.removeIf(s -> !inJar.getPropertyNames().contains(s) && !s.startsWith("quarkus."));

        Consumer<BuildChainBuilder> result = Functions.discardingConsumer();
        result = result.andThen(bcb -> bcb.addBuildStep(bc -> {
            bc.produce(new BuildTimeConfigurationBuildItem(buildTimeConfig));
            bc.produce(new BuildTimeRunTimeFixedConfigurationBuildItem(buildTimeRunTimeConfig));
            bc.produce(new RunTimeConfigurationBuildItem(runTimeConfig));
            bc.produce(new UnmatchedConfigBuildItem(Collections.unmodifiableSet(unmatched)));
        }).produces(BuildTimeConfigurationBuildItem.class)
                .produces(BuildTimeRunTimeFixedConfigurationBuildItem.class)
                .produces(RunTimeConfigurationBuildItem.class)
                .produces(UnmatchedConfigBuildItem.class)
                .build());
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, "META-INF/quarkus-build-steps.list")) {
            result = result.andThen(ExtensionLoader.loadStepsFrom(clazz, buildTimeConfig, buildTimeRunTimeConfig, launchMode));
        }
        return result;
    }

    /**
     * Load all the build steps from the given class.
     *
     * @param clazz the class to load from (must not be {@code null})
     * @param buildTimeConfig the build time configuration (must not be {@code null})
     * @param buildTimeRunTimeConfig the build time/run time visible config (must not be {@code null})
     * @param launchMode
     * @return a consumer which adds the steps to the given chain builder
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(Class<?> clazz, ConfigDefinition buildTimeConfig,
            ConfigDefinition buildTimeRunTimeConfig, final LaunchMode launchMode) {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // this is the chain configuration that will contain all steps on this class and be returned
        Consumer<BuildChainBuilder> chainConfig = Functions.discardingConsumer();
        // this is the step configuration that applies to all steps on this class
        Consumer<BuildStepBuilder> stepConfig = Functions.discardingConsumer();
        // this is the build step instance setup that applies to all steps on this class
        BiConsumer<BuildContext, Object> stepInstanceSetup = Functions.discardingBiConsumer();
        Map<Class<? extends BooleanSupplier>, BooleanSupplier> condCache = new HashMap<>();

        if (constructors.length != 1) {
            throw reportError(clazz, "Build step classes must have exactly one constructor");
        }

        EnumSet<ConfigPhase> consumingConfigPhases = EnumSet.noneOf(ConfigPhase.class);

        final Constructor<?> constructor = constructors[0];
        if (!(Modifier.isPublic(constructor.getModifiers())))
            constructor.setAccessible(true);
        final Parameter[] ctorParameters = constructor.getParameters();
        final List<Function<BuildContext, Object>> ctorParamFns;
        if (ctorParameters.length == 0) {
            ctorParamFns = Collections.emptyList();
        } else {
            ctorParamFns = new ArrayList<>(ctorParameters.length);
            for (Parameter parameter : ctorParameters) {
                Type parameterType = parameter.getParameterizedType();
                final Class<?> parameterClass = parameter.getType();
                final boolean weak = parameter.isAnnotationPresent(Weak.class);
                if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(parameterType)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consume(buildItemClass));
                } else if (isListOf(parameterType, MultiBuildItem.class)) {
                    final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(MultiBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consumeMulti(buildItemClass));
                } else if (isConsumerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    if (weak) {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    }
                    ctorParamFns.add(bc -> (Consumer<? extends BuildItem>) bc::produce);
                } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    if (weak) {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    }
                    ctorParamFns.add(bc -> (BuildProducer<? extends BuildItem>) bc::produce);
                } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> Optional.ofNullable(bc.consume(buildItemClass)));
                } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(
                            rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                    ctorParamFns.add(bc -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional
                            .ofNullable(bc.consume(buildItemClass)));
                } else if (rawTypeOf(parameterType) == Executor.class) {
                    ctorParamFns.add(BuildContext::getExecutor);
                } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                    final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                    final ConfigPhase phase = annotation.phase();
                    consumingConfigPhases.add(phase);

                    if (phase == ConfigPhase.BUILD_TIME) {
                        ctorParamFns.add(bc -> bc.consume(BuildTimeConfigurationBuildItem.class).getConfigDefinition()
                                .getRealizedInstance(parameterClass));
                    } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        ctorParamFns.add(bc -> bc.consume(BuildTimeRunTimeFixedConfigurationBuildItem.class)
                                .getConfigDefinition().getRealizedInstance(parameterClass));
                    } else if (phase == ConfigPhase.RUN_TIME) {
                        ctorParamFns.add(bc -> bc.consume(RunTimeConfigurationBuildItem.class).getConfigDefinition()
                                .getRealizedInstance(parameterClass));
                    } else {
                        throw reportError(parameterClass, "Unknown value for ConfigPhase");
                    }
                } else if (isRecorder(parameterClass)) {
                    throw reportError(parameter, "Bytecode recorders disallowed on constructor parameters");
                } else {
                    throw reportError(parameter, "Unsupported constructor parameter type " + parameterType);
                }
            }
        }

        // index fields
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            final int mods = field.getModifiers();
            if (Modifier.isStatic(mods)) {
                // ignore static fields
                continue;
            }
            if (Modifier.isFinal(mods)) {
                // ignore final fields
                continue;
            }
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                field.setAccessible(true);
            }
            // next, determine the type
            final Type fieldType = field.getGenericType();
            final Class<?> fieldClass = field.getType();
            final boolean weak = field.isAnnotationPresent(Weak.class);
            if (rawTypeExtends(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(fieldType).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consume(buildItemClass)));
            } else if (isListOf(fieldType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(MultiBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consumeMulti(buildItemClass)));
            } else if (isConsumerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                if (weak) {
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                } else {
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                }
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Consumer<? extends BuildItem>) bc::produce));
            } else if (isBuildProducerOf(fieldType, BuildItem.class)) {
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                if (weak) {
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                } else {
                    stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                }
                stepInstanceSetup = stepInstanceSetup.andThen(
                        (bc, o) -> ReflectUtil.setFieldVal(field, o, (BuildProducer<? extends BuildItem>) bc::produce));
            } else if (isOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (isSupplierOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o,
                        (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass)));
            } else if (isSupplierOfOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(rawTypeOfParameter(fieldType, 0), 0)
                        .asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o,
                        (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional.ofNullable(bc.consume(buildItemClass))));
            } else if (fieldClass == Executor.class) {
                stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.getExecutor()));
            } else if (fieldClass.isAnnotationPresent(ConfigRoot.class)) {
                final ConfigRoot annotation = fieldClass.getAnnotation(ConfigRoot.class);
                final ConfigPhase phase = annotation.phase();
                consumingConfigPhases.add(phase);

                if (phase == ConfigPhase.BUILD_TIME) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final BuildTimeConfigurationBuildItem configurationBuildItem = bc
                                .consume(BuildTimeConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final BuildTimeRunTimeFixedConfigurationBuildItem configurationBuildItem = bc
                                .consume(BuildTimeRunTimeFixedConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else if (phase == ConfigPhase.RUN_TIME) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final RunTimeConfigurationBuildItem configurationBuildItem = bc
                                .consume(RunTimeConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getConfigDefinition().getRealizedInstance(fieldClass));
                    });
                } else {
                    throw reportError(fieldClass, "Unknown value for ConfigPhase");
                }
            } else if (isRecorder(fieldClass)) {
                throw reportError(field, "Bytecode recorders disallowed on fields");
            } else {
                throw reportError(field, "Unsupported field type " + fieldType);
            }
        }

        // now iterate the methods
        final Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            final int mods = method.getModifiers();
            if (Modifier.isStatic(mods)) {
                continue;
            }
            if (!method.isAnnotationPresent(BuildStep.class))
                continue;
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            final BuildStep buildStep = method.getAnnotation(BuildStep.class);
            final String[] archiveMarkers = buildStep.applicationArchiveMarkers();
            final String[] capabilities = buildStep.providesCapabilities();
            final Class<? extends BooleanSupplier>[] onlyIf = buildStep.onlyIf();
            final Class<? extends BooleanSupplier>[] onlyIfNot = buildStep.onlyIfNot();
            final Parameter[] methodParameters = method.getParameters();
            final Record recordAnnotation = method.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            final List<BiFunction<BuildContext, BytecodeRecorderImpl, Object>> methodParamFns;
            Consumer<BuildStepBuilder> methodStepConfig = Functions.discardingConsumer();
            BooleanSupplier addStep = () -> true;
            for (boolean inv : new boolean[] { false, true }) {
                Class<? extends BooleanSupplier>[] testClasses = inv ? onlyIfNot : onlyIf;
                for (Class<? extends BooleanSupplier> testClass : testClasses) {
                    BooleanSupplier bs = condCache.get(testClass);
                    if (bs == null) {
                        // construct a new supplier instance
                        Consumer<BooleanSupplier> setup = o -> {
                        };
                        final Constructor<?>[] ctors = testClass.getDeclaredConstructors();
                        if (ctors.length != 1) {
                            throw reportError(testClass, "Conditional class must declare exactly one constructor");
                        }
                        final Constructor<?> ctor = ctors[0];
                        ctor.setAccessible(true);
                        List<Supplier<?>> paramSuppList = new ArrayList<>();
                        for (Parameter parameter : ctor.getParameters()) {
                            final Class<?> parameterClass = parameter.getType();
                            if (parameterClass == LaunchMode.class) {
                                paramSuppList.add(() -> launchMode);
                            } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                                final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                                final ConfigPhase phase = annotation.phase();
                                ConfigDefinition confDef;
                                if (phase == ConfigPhase.BUILD_TIME) {
                                    confDef = buildTimeConfig;
                                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                                    confDef = buildTimeRunTimeConfig;
                                } else {
                                    throw reportError(parameter,
                                            "Unsupported conditional class configuration build phase " + phase);
                                }
                                paramSuppList.add(() -> confDef.getRealizedInstance(parameterClass));
                            } else {
                                throw reportError(parameter,
                                        "Unsupported conditional class constructor parameter type " + parameterClass);
                            }
                        }
                        for (Field field : testClass.getDeclaredFields()) {
                            final int fieldMods = field.getModifiers();
                            if (Modifier.isStatic(fieldMods)) {
                                // ignore static fields
                                continue;
                            }
                            if (Modifier.isFinal(fieldMods)) {
                                // ignore final fields
                                continue;
                            }
                            if (!Modifier.isPublic(fieldMods) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                                field.setAccessible(true);
                            }
                            final Class<?> fieldClass = field.getType();
                            if (fieldClass == LaunchMode.class) {
                                setup = setup.andThen(o -> ReflectUtil.setFieldVal(field, o, launchMode));
                            } else if (fieldClass.isAnnotationPresent(ConfigRoot.class)) {
                                final ConfigRoot annotation = fieldClass.getAnnotation(ConfigRoot.class);
                                final ConfigPhase phase = annotation.phase();
                                ConfigDefinition confDef;
                                if (phase == ConfigPhase.BUILD_TIME) {
                                    confDef = buildTimeConfig;
                                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                                    confDef = buildTimeRunTimeConfig;
                                } else {
                                    throw reportError(field,
                                            "Unsupported conditional class configuration build phase " + phase);
                                }
                                setup = setup.andThen(
                                        o -> ReflectUtil.setFieldVal(field, o, confDef.getRealizedInstance(fieldClass)));
                            } else {
                                throw reportError(field, "Unsupported conditional class field type " + fieldClass);
                            }
                        }
                        // make it
                        Object[] args = new Object[paramSuppList.size()];
                        int idx = 0;
                        for (Supplier<?> supplier : paramSuppList) {
                            args[idx++] = supplier.get();
                        }
                        try {
                            bs = (BooleanSupplier) ctor.newInstance(args);
                        } catch (InstantiationException e) {
                            throw ReflectUtil.toError(e);
                        } catch (IllegalAccessException e) {
                            throw ReflectUtil.toError(e);
                        } catch (InvocationTargetException e) {
                            try {
                                throw e.getCause();
                            } catch (RuntimeException | Error e2) {
                                throw e2;
                            } catch (Throwable throwable) {
                                throw new IllegalStateException(throwable);
                            }
                        }
                        setup.accept(bs);
                        condCache.put(testClass, bs);
                    }
                    if (inv) {
                        addStep = and(addStep, not(bs));
                    } else {
                        addStep = and(addStep, bs);
                    }
                }
            }
            final BooleanSupplier finalAddStep = addStep;

            if (archiveMarkers.length > 0) {
                chainConfig = chainConfig.andThen(bcb -> bcb.addBuildStep(bc -> {
                    for (String marker : archiveMarkers) {
                        bc.produce(new AdditionalApplicationArchiveMarkerBuildItem(marker));
                    }
                }).produces(AdditionalApplicationArchiveMarkerBuildItem.class).buildIf(finalAddStep));
            }
            if (capabilities.length > 0) {
                chainConfig = chainConfig.andThen(bcb -> bcb.addBuildStep(bc -> {
                    for (String capability : capabilities) {
                        bc.produce(new CapabilityBuildItem(capability));
                    }
                }).produces(CapabilityBuildItem.class).buildIf(finalAddStep));
            }

            if (isRecorder) {
                assert recordAnnotation != null;
                final ExecutionTime executionTime = recordAnnotation.value();
                final boolean optional = recordAnnotation.optional();
                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(
                        executionTime == ExecutionTime.STATIC_INIT ? StaticBytecodeRecorderBuildItem.class
                                : MainBytecodeRecorderBuildItem.class,
                        optional ? ProduceFlags.of(ProduceFlag.WEAK) : ProduceFlags.NONE));
            }
            EnumSet<ConfigPhase> methodConsumingConfigPhases = consumingConfigPhases.clone();
            if (methodParameters.length == 0) {
                methodParamFns = Collections.emptyList();
            } else {
                methodParamFns = new ArrayList<>(methodParameters.length);
                for (Parameter parameter : methodParameters) {
                    final boolean weak = parameter.isAnnotationPresent(Weak.class);
                    final Type parameterType = parameter.getParameterizedType();
                    final Class<?> parameterClass = parameter.getType();
                    if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = parameterClass
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consume(buildItemClass));
                    } else if (isListOf(parameterType, MultiBuildItem.class)) {
                        final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(MultiBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> bc.consumeMulti(buildItemClass));
                    } else if (isConsumerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(BuildItem.class);
                        if (weak) {
                            methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                        } else {
                            methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        }
                        methodParamFns.add((bc, bri) -> (Consumer<? extends BuildItem>) bc::produce);
                    } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(BuildItem.class);
                        if (weak) {
                            methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                        } else {
                            methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        }
                        methodParamFns.add((bc, bri) -> (BuildProducer<? extends BuildItem>) bc::produce);
                    } else if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> Optional.ofNullable(bc.consume(buildItemClass)));
                    } else if (isSupplierOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                        methodParamFns.add((bc, bri) -> (Supplier<? extends SimpleBuildItem>) () -> bc.consume(buildItemClass));
                    } else if (isSupplierOfOptionalOf(parameterType, SimpleBuildItem.class)) {
                        final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(
                                rawTypeOfParameter(parameterType, 0), 0).asSubclass(SimpleBuildItem.class);
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.consumes(buildItemClass, ConsumeFlags.of(ConsumeFlag.OPTIONAL)));
                        methodParamFns.add((bc, bri) -> (Supplier<Optional<? extends SimpleBuildItem>>) () -> Optional
                                .ofNullable(bc.consume(buildItemClass)));
                    } else if (rawTypeOf(parameterType) == Executor.class) {
                        methodParamFns.add((bc, bri) -> bc.getExecutor());
                    } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                        final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                        final ConfigPhase phase = annotation.phase();
                        methodConsumingConfigPhases.add(phase);

                        if (phase == ConfigPhase.BUILD_TIME) {
                            methodParamFns.add((bc, bri) -> {
                                final BuildTimeConfigurationBuildItem configurationBuildItem = bc
                                        .consume(BuildTimeConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                            methodParamFns.add((bc, bri) -> {
                                final BuildTimeRunTimeFixedConfigurationBuildItem configurationBuildItem = bc
                                        .consume(BuildTimeRunTimeFixedConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else if (phase == ConfigPhase.RUN_TIME) {
                            methodParamFns.add((bc, bri) -> {
                                final RunTimeConfigurationBuildItem configurationBuildItem = bc
                                        .consume(RunTimeConfigurationBuildItem.class);
                                return configurationBuildItem.getConfigDefinition().getRealizedInstance(parameterClass);
                            });
                        } else {
                            throw reportError(parameterClass, "Unknown value for ConfigPhase");
                        }
                    } else if (isRecorder(parameter.getType())) {
                        if (!isRecorder) {
                            throw reportError(parameter,
                                    "Cannot pass recorders to method which is not annotated with " + Record.class);
                        }
                        methodParamFns.add((bc, bri) -> {
                            assert bri != null;
                            return bri.getRecordingProxy(parameterClass);
                        });
                    } else if (parameter.getType() == RecorderContext.class
                            || parameter.getType() == BytecodeRecorderImpl.class) {
                        if (!isRecorder) {
                            throw reportError(parameter,
                                    "Cannot pass recorder context to method which is not annotated with " + Record.class);
                        }
                        methodParamFns.add((bc, bri) -> bri);
                    } else {
                        throw reportError(parameter, "Unsupported method parameter " + parameterType);
                    }
                }
            }

            final BiConsumer<BuildContext, Object> resultConsumer;
            final Type returnType = method.getGenericReturnType();
            final boolean weak = method.isAnnotationPresent(Weak.class);
            if (rawTypeIs(returnType, void.class)) {
                resultConsumer = Functions.discardingBiConsumer();
            } else if (rawTypeExtends(returnType, BuildItem.class)) {
                final Class<? extends BuildItem> type = method.getReturnType().asSubclass(BuildItem.class);
                if (weak) {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                } else {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                }
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((BuildItem) o);
                };
            } else if (isOptionalOf(returnType, BuildItem.class)) {
                final Class<? extends BuildItem> type = rawTypeOfParameter(returnType, 0).asSubclass(BuildItem.class);
                if (weak) {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                } else {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                }
                resultConsumer = (bc, o) -> ((Optional<? extends BuildItem>) o).ifPresent(bc::produce);
            } else if (isListOf(returnType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> type = rawTypeOfParameter(returnType, 0).asSubclass(MultiBuildItem.class);
                if (weak) {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                } else {
                    methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                }
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((List<? extends MultiBuildItem>) o);
                };
            } else {
                throw reportError(method, "Unsupported method return type " + returnType);
            }

            if (methodConsumingConfigPhases.contains(ConfigPhase.RUN_TIME)) {
                if (isRecorder && recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(RunTimeConfigurationBuildItem.class));
            }
            if (methodConsumingConfigPhases.contains(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(BuildTimeRunTimeFixedConfigurationBuildItem.class));
            }
            if (methodConsumingConfigPhases.contains(ConfigPhase.BUILD_TIME)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(BuildTimeConfigurationBuildItem.class));
            }

            final Consume[] consumes = method.getAnnotationsByType(Consume.class);
            if (consumes.length > 0) {
                stepConfig = stepConfig.andThen(bsb -> {
                    for (Consume consume : consumes) {
                        bsb.afterProduce(consume.value());
                    }
                });
            }
            final Produce[] produces = method.getAnnotationsByType(Produce.class);
            if (produces.length > 0) {
                stepConfig = stepConfig.andThen(bsb -> {
                    for (Produce produce : produces) {
                        bsb.beforeConsume(produce.value());
                    }
                });
            }

            final Consumer<BuildStepBuilder> finalStepConfig = stepConfig.andThen(methodStepConfig)
                    .andThen(buildStepBuilder -> buildStepBuilder.buildIf(finalAddStep));
            final BiConsumer<BuildContext, Object> finalStepInstanceSetup = stepInstanceSetup;
            final String name = clazz.getName() + "#" + method.getName();
            chainConfig = chainConfig
                    .andThen(bcb -> finalStepConfig.accept(bcb.addBuildStep(new io.quarkus.builder.BuildStep() {
                        public void execute(final BuildContext bc) {
                            Object[] ctorArgs = new Object[ctorParamFns.size()];
                            for (int i = 0; i < ctorArgs.length; i++) {
                                ctorArgs[i] = ctorParamFns.get(i).apply(bc);
                            }
                            Object instance;
                            try {
                                instance = constructor.newInstance(ctorArgs);
                            } catch (InstantiationException e) {
                                throw ReflectUtil.toError(e);
                            } catch (IllegalAccessException e) {
                                throw ReflectUtil.toError(e);
                            } catch (InvocationTargetException e) {
                                try {
                                    throw e.getCause();
                                } catch (RuntimeException | Error e2) {
                                    throw e2;
                                } catch (Throwable t) {
                                    throw new IllegalStateException(t);
                                }
                            }
                            finalStepInstanceSetup.accept(bc, instance);
                            Object[] methodArgs = new Object[methodParamFns.size()];
                            BytecodeRecorderImpl bri = isRecorder
                                    ? new BytecodeRecorderImpl(recordAnnotation.value() == ExecutionTime.STATIC_INIT,
                                            clazz.getSimpleName(), method.getName())
                                    : null;
                            for (int i = 0; i < methodArgs.length; i++) {
                                methodArgs[i] = methodParamFns.get(i).apply(bc, bri);
                            }
                            Object result;
                            try {
                                result = method.invoke(instance, methodArgs);
                            } catch (IllegalAccessException e) {
                                throw ReflectUtil.toError(e);
                            } catch (InvocationTargetException e) {
                                try {
                                    throw e.getCause();
                                } catch (RuntimeException | Error e2) {
                                    throw e2;
                                } catch (Throwable t) {
                                    throw new IllegalStateException(t);
                                }
                            }
                            resultConsumer.accept(bc, result);
                            if (isRecorder) {
                                // commit recorded data
                                if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                                    bc.produce(new StaticBytecodeRecorderBuildItem(bri));
                                } else {
                                    bc.produce(new MainBytecodeRecorderBuildItem(bri));
                                }

                            }
                        }

                        public String toString() {
                            return name;
                        }
                    })));
        }
        return chainConfig;
    }

    private static BooleanSupplier and(BooleanSupplier a, BooleanSupplier b) {
        return () -> a.getAsBoolean() && b.getAsBoolean();
    }

    private static BooleanSupplier not(BooleanSupplier x) {
        return () -> !x.getAsBoolean();
    }

    private static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
        if (e instanceof Member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Member) e).getDeclaringClass());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of "
                    + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }
}
