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
import static java.util.Arrays.asList;

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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.wildfly.common.function.Functions;

import io.quarkus.bootstrap.model.AppModel;
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
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.ProduceWeak;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.BootstrapConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationProxyBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.DefaultValuesConfigurationSource;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.recording.ObjectLoader;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.KeyMap;
import io.smallrye.config.KeyMapBackedConfigSource;
import io.smallrye.config.NameIterator;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Utility class to load build steps, runtime recorders, and configuration roots from a given extension class.
 */
public final class ExtensionLoader {
    private ExtensionLoader() {
    }

    private static final Logger loadLog = Logger.getLogger("io.quarkus.deployment");
    private static final Logger cfgLog = Logger.getLogger("io.quarkus.configuration");
    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    @SuppressWarnings("deprecation")
    private static boolean isRecorder(AnnotatedElement element) {
        return element.isAnnotationPresent(Recorder.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @param buildSystemProps the build system properties to use
     * @param platformProperties Quarkus platform properties
     * @param launchMode launch mode
     * @param configCustomizer configuration customizer
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, Properties buildSystemProps,
            AppModel appModel, LaunchMode launchMode, DevModeType devModeType,
            Consumer<ConfigBuilder> configCustomizer)
            throws IOException, ClassNotFoundException {
        // populate with all known types
        List<Class<?>> roots = new ArrayList<>();
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, CONFIG_ROOTS_LIST)) {
            final ConfigRoot annotation = clazz.getAnnotation(ConfigRoot.class);
            if (annotation == null) {
                cfgLog.warnf("Ignoring configuration root %s because it has no annotation", clazz);
            } else {
                roots.add(clazz);
            }
        }

        final BuildTimeConfigurationReader reader = new BuildTimeConfigurationReader(roots);

        // now prepare & load the build configuration
        final SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false, launchMode);

        final DefaultValuesConfigurationSource ds1 = new DefaultValuesConfigurationSource(
                reader.getBuildTimePatternMap());
        final DefaultValuesConfigurationSource ds2 = new DefaultValuesConfigurationSource(
                reader.getBuildTimeRunTimePatternMap());
        final PropertiesConfigSource pcs = new PropertiesConfigSource(buildSystemProps, "Build system");
        final Map<String, String> platformProperties = appModel.getPlatformProperties();
        if (platformProperties.isEmpty()) {
            builder.withSources(ds1, ds2, pcs);
        } else {
            final KeyMap<String> props = new KeyMap<>(platformProperties.size());
            for (Map.Entry<String, String> prop : platformProperties.entrySet()) {
                props.findOrAdd(new NameIterator(prop.getKey())).putRootValue(prop.getValue());
            }
            final KeyMapBackedConfigSource platformConfigSource = new KeyMapBackedConfigSource("Quarkus platform",
                    // Our default value configuration source is using an ordinal of Integer.MIN_VALUE
                    // (see io.quarkus.deployment.configuration.DefaultValuesConfigurationSource)
                    Integer.MIN_VALUE + 1000, props);
            builder.withSources(ds1, ds2, platformConfigSource, pcs);
        }

        if (configCustomizer != null) {
            configCustomizer.accept(builder);
        }
        final SmallRyeConfig src = builder.build();

        // install globally
        QuarkusConfigFactory.setConfig(src);
        final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            cpr.releaseConfig(cpr.getConfig());
        } catch (IllegalStateException ignored) {
            // just means no config was installed, which is fine
        }

        final BuildTimeConfigurationReader.ReadResult readResult = reader.readConfiguration(src);
        final BooleanSupplierFactoryBuildItem bsf = new BooleanSupplierFactoryBuildItem(readResult, launchMode, devModeType);

        Consumer<BuildChainBuilder> result = Functions.discardingConsumer();
        // BooleanSupplier factory
        result = result.andThen(bcb -> bcb.addBuildStep(bc -> {
            bc.produce(bsf);
        }).produces(BooleanSupplierFactoryBuildItem.class).build());

        // the proxy objects used for run time config in the recorders
        Map<Class<?>, Object> proxies = new HashMap<>();
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, "META-INF/quarkus-build-steps.list")) {
            try {
                result = result.andThen(ExtensionLoader.loadStepsFromClass(clazz, readResult, proxies, bsf));
            } catch (Throwable e) {
                throw new RuntimeException("Failed to load steps from " + clazz, e);
            }
        }

        // this has to be an identity hash map else the recorder will get angry
        Map<Object, FieldDescriptor> proxyFields = new IdentityHashMap<>();
        for (Map.Entry<Class<?>, Object> entry : proxies.entrySet()) {
            final RootDefinition def = readResult.requireRootDefinitionForClass(entry.getKey());
            proxyFields.put(entry.getValue(), def.getDescriptor());
        }
        result = result.andThen(bcb -> bcb.addBuildStep(bc -> {
            bc.produce(new ConfigurationBuildItem(readResult));
            bc.produce(new RunTimeConfigurationProxyBuildItem(proxies));
            final ObjectLoader loader = new ObjectLoader() {
                public ResultHandle load(final BytecodeCreator body, final Object obj, final boolean staticInit) {
                    return body.readStaticField(proxyFields.get(obj));
                }

                public boolean canHandleObject(final Object obj, final boolean staticInit) {
                    return proxyFields.containsKey(obj);
                }
            };
            bc.produce(new BytecodeRecorderObjectLoaderBuildItem(loader));
        }).produces(ConfigurationBuildItem.class)
                .produces(RunTimeConfigurationProxyBuildItem.class)
                .produces(BytecodeRecorderObjectLoaderBuildItem.class)
                .build());

        return result;
    }

    /**
     * Load all the build steps from the given class.
     *
     * @param clazz the class to load from (must not be {@code null})
     * @param readResult the build time configuration read result (must not be {@code null})
     * @param runTimeProxies the map of run time proxy objects to populate for recorders (must not be {@code null})
     * @param launchMode the launch mode
     * @return a consumer which adds the steps to the given chain builder
     */
    private static Consumer<BuildChainBuilder> loadStepsFromClass(Class<?> clazz,
            BuildTimeConfigurationReader.ReadResult readResult,
            Map<Class<?>, Object> runTimeProxies, BooleanSupplierFactoryBuildItem supplierFactory) {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // this is the chain configuration that will contain all steps on this class and be returned
        Consumer<BuildChainBuilder> chainConfig = Functions.discardingConsumer();
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return chainConfig;
        }
        // this is the step configuration that applies to all steps on this class
        Consumer<BuildStepBuilder> stepConfig = Functions.discardingConsumer();
        // this is the build step instance setup that applies to all steps on this class
        BiConsumer<BuildContext, Object> stepInstanceSetup = Functions.discardingBiConsumer();

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
                final boolean overridable = parameter.isAnnotationPresent(Overridable.class);
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
                    deprecatedProducer(parameter);
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    if (overridable) {
                        if (weak) {
                            stepConfig = stepConfig
                                    .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                        } else {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                        }
                    } else {
                        if (weak) {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                        } else {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        }
                    }
                    ctorParamFns.add(bc -> (Consumer<? extends BuildItem>) bc::produce);
                } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                    deprecatedProducer(parameter);
                    final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(BuildItem.class);
                    if (overridable) {
                        if (weak) {
                            stepConfig = stepConfig
                                    .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                        } else {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                        }
                    } else {
                        if (weak) {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                        } else {
                            stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                        }
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

                    if (phase.isAvailableAtBuild()) {
                        ctorParamFns.add(bc -> bc.consume(ConfigurationBuildItem.class).getReadResult()
                                .requireRootObjectForClass(parameterClass));
                        if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                            runTimeProxies.computeIfAbsent(parameterClass, readResult::requireRootObjectForClass);
                        }
                    } else if (phase.isReadAtMain()) {
                        throw reportError(parameter, phase + " configuration cannot be consumed here");
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
            final boolean overridable = field.isAnnotationPresent(Overridable.class);
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
                deprecatedProducer(field);
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                if (overridable) {
                    if (weak) {
                        stepConfig = stepConfig
                                .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                    }
                } else {
                    if (weak) {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    }
                }
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, (Consumer<? extends BuildItem>) bc::produce));
            } else if (isBuildProducerOf(fieldType, BuildItem.class)) {
                deprecatedProducer(field);
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0).asSubclass(BuildItem.class);
                if (overridable) {
                    if (weak) {
                        stepConfig = stepConfig
                                .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                    }
                } else {
                    if (weak) {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                    } else {
                        stepConfig = stepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                    }
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

                if (phase.isAvailableAtBuild()) {
                    stepInstanceSetup = stepInstanceSetup.andThen((bc, o) -> {
                        final ConfigurationBuildItem configurationBuildItem = bc
                                .consume(ConfigurationBuildItem.class);
                        ReflectUtil.setFieldVal(field, o,
                                configurationBuildItem.getReadResult().requireRootObjectForClass(fieldClass));
                    });
                    if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        runTimeProxies.computeIfAbsent(fieldClass, readResult::requireRootObjectForClass);
                    }
                } else if (phase.isReadAtMain()) {
                    throw reportError(field, phase + " configuration cannot be consumed here");
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
        final List<Method> methods = getMethods(clazz);
        for (Method method : methods) {
            final BuildStep buildStep = method.getAnnotation(BuildStep.class);
            if (buildStep == null) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                throw new RuntimeException("A build step must be a non-static method: " + method);
            }
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            final Class<? extends BooleanSupplier>[] onlyIf = buildStep.onlyIf();
            final Class<? extends BooleanSupplier>[] onlyIfNot = buildStep.onlyIfNot();
            final Parameter[] methodParameters = method.getParameters();
            final Record recordAnnotation = method.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            final boolean identityComparison = isRecorder ? recordAnnotation.useIdentityComparisonForParameters() : true;
            if (isRecorder) {
                boolean recorderFound = false;
                for (Class<?> p : method.getParameterTypes()) {
                    if (isRecorder(p)) {
                        recorderFound = true;
                        break;
                    }
                }
                if (!recorderFound) {
                    throw new RuntimeException(method + " is marked @Record but does not inject an @Recorder object");
                }
            }
            final List<BiFunction<BuildContext, BytecodeRecorderImpl, Object>> methodParamFns;
            Consumer<BuildStepBuilder> methodStepConfig = Functions.discardingConsumer();
            BooleanSupplier addStep = () -> true;
            for (boolean inv : new boolean[] { false, true }) {
                Class<? extends BooleanSupplier>[] testClasses = inv ? onlyIfNot : onlyIf;
                for (Class<? extends BooleanSupplier> testClass : testClasses) {
                    BooleanSupplier bs = supplierFactory.get((Class<? extends BooleanSupplier>) testClass);
                    if (inv) {
                        addStep = and(addStep, not(bs));
                    } else {
                        addStep = and(addStep, bs);
                    }
                }
            }
            final BooleanSupplier finalAddStep = addStep;

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
                    final boolean overridable = parameter.isAnnotationPresent(Overridable.class);
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
                        if (overridable) {
                            if (weak) {
                                methodStepConfig = methodStepConfig.andThen(
                                        bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                            } else {
                                methodStepConfig = methodStepConfig
                                        .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                            }
                        } else {
                            if (weak) {
                                methodStepConfig = methodStepConfig
                                        .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                            } else {
                                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                            }
                        }
                        methodParamFns.add((bc, bri) -> (Consumer<? extends BuildItem>) bc::produce);
                    } else if (isBuildProducerOf(parameterType, BuildItem.class)) {
                        final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                                .asSubclass(BuildItem.class);
                        if (overridable) {
                            if (weak) {
                                methodStepConfig = methodStepConfig.andThen(
                                        bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                            } else {
                                methodStepConfig = methodStepConfig
                                        .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.OVERRIDABLE));
                            }
                        } else {
                            if (weak) {
                                methodStepConfig = methodStepConfig
                                        .andThen(bsb -> bsb.produces(buildItemClass, ProduceFlag.WEAK));
                            } else {
                                methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(buildItemClass));
                            }
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

                        if (phase.isAvailableAtBuild()) {
                            methodParamFns.add((bc, bri) -> {
                                final ConfigurationBuildItem configurationBuildItem = bc
                                        .consume(ConfigurationBuildItem.class);
                                return configurationBuildItem.getReadResult().requireRootObjectForClass(parameterClass);
                            });
                            if (isRecorder && phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                                runTimeProxies.computeIfAbsent(parameterClass, readResult::requireRootObjectForClass);
                            }
                        } else if (phase.isReadAtMain()) {
                            if (isRecorder) {
                                methodParamFns.add((bc, bri) -> {
                                    final RunTimeConfigurationProxyBuildItem proxies = bc
                                            .consume(RunTimeConfigurationProxyBuildItem.class);
                                    return proxies.getProxyObjectFor(parameterClass);
                                });
                                runTimeProxies.computeIfAbsent(parameterClass, ReflectUtil::newInstance);
                            } else {
                                throw reportError(parameter,
                                        phase + " configuration cannot be consumed here unless the method is a @Recorder");
                            }
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
            final boolean overridable = method.isAnnotationPresent(Overridable.class);
            if (rawTypeIs(returnType, void.class)) {
                resultConsumer = Functions.discardingBiConsumer();
            } else if (rawTypeExtends(returnType, BuildItem.class)) {
                final Class<? extends BuildItem> type = method.getReturnType().asSubclass(BuildItem.class);
                if (overridable) {
                    if (weak) {
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE));
                    }
                } else {
                    if (weak) {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                    }
                }
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((BuildItem) o);
                };
            } else if (isOptionalOf(returnType, BuildItem.class)) {
                final Class<? extends BuildItem> type = rawTypeOfParameter(returnType, 0).asSubclass(BuildItem.class);
                if (overridable) {
                    if (weak) {
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE));
                    }
                } else {
                    if (weak) {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                    }
                }
                resultConsumer = (bc, o) -> ((Optional<? extends BuildItem>) o).ifPresent(bc::produce);
            } else if (isListOf(returnType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> type = rawTypeOfParameter(returnType, 0).asSubclass(MultiBuildItem.class);
                if (overridable) {
                    if (weak) {
                        methodStepConfig = methodStepConfig
                                .andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.OVERRIDABLE));
                    }
                } else {
                    if (weak) {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type, ProduceFlag.WEAK));
                    } else {
                        methodStepConfig = methodStepConfig.andThen(bsb -> bsb.produces(type));
                    }
                }
                resultConsumer = (bc, o) -> {
                    if (o != null)
                        bc.produce((List<? extends MultiBuildItem>) o);
                };
            } else {
                throw reportError(method, "Unsupported method return type " + returnType);
            }

            if (methodConsumingConfigPhases.contains(ConfigPhase.BOOTSTRAP)
                    || methodConsumingConfigPhases.contains(ConfigPhase.RUN_TIME)) {
                if (isRecorder && recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }

                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(RunTimeConfigurationProxyBuildItem.class));

                if (methodConsumingConfigPhases.contains(ConfigPhase.BOOTSTRAP)) {
                    methodStepConfig = methodStepConfig
                            .andThen(bsb -> bsb.afterProduce(BootstrapConfigSetupCompleteBuildItem.class));
                }
                if (methodConsumingConfigPhases.contains(ConfigPhase.RUN_TIME)) {
                    methodStepConfig = methodStepConfig
                            .andThen(bsb -> bsb.afterProduce(RuntimeConfigSetupCompleteBuildItem.class));
                }
            }

            if (methodConsumingConfigPhases.contains(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
                    || methodConsumingConfigPhases.contains(ConfigPhase.BUILD_TIME)) {
                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(ConfigurationBuildItem.class));
            }

            final Consume[] consumes = method.getAnnotationsByType(Consume.class);
            if (consumes.length > 0) {
                methodStepConfig = methodStepConfig.andThen(bsb -> {
                    for (Consume consume : consumes) {
                        bsb.afterProduce(consume.value());
                    }
                });
            }
            final Produce[] produces = method.getAnnotationsByType(Produce.class);
            if (produces.length > 0) {
                methodStepConfig = methodStepConfig.andThen(bsb -> {
                    for (Produce produce : produces) {
                        bsb.beforeConsume(produce.value());
                    }
                });
            }
            final ProduceWeak[] produceWeaks = method.getAnnotationsByType(ProduceWeak.class);
            if (produceWeaks.length > 0) {
                methodStepConfig = methodStepConfig.andThen(bsb -> {
                    for (ProduceWeak produceWeak : produceWeaks) {
                        bsb.beforeConsume(produceWeak.value(), ProduceFlag.WEAK);
                    }
                });
            }
            final Consumer<BuildStepBuilder> finalStepConfig = stepConfig.andThen(methodStepConfig)
                    .andThen(buildStepBuilder -> buildStepBuilder.buildIf(finalAddStep));
            final BiConsumer<BuildContext, Object> finalStepInstanceSetup = stepInstanceSetup;
            final String name = clazz.getName() + "#" + method.getName();

            chainConfig = chainConfig
                    .andThen(bcb -> {
                        BuildStepBuilder bsb = bcb.addBuildStep(new io.quarkus.builder.BuildStep() {
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
                                                clazz.getSimpleName(), method.getName(),
                                                Integer.toString(method.toString().hashCode()), identityComparison)
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
                        });
                        finalStepConfig.accept(bsb);
                    });
        }
        return chainConfig;
    }

    private static void deprecatedProducer(final Object element) {
        loadLog.warnf(
                "Producing values from constructors and fields is no longer supported and will be removed in a future release: %s",
                element);
    }

    protected static List<Method> getMethods(Class<?> clazz) {
        List<Method> declaredMethods = new ArrayList<>();
        if (!clazz.getName().equals(Object.class.getName())) {
            declaredMethods.addAll(getMethods(clazz.getSuperclass()));
            declaredMethods.addAll(asList(clazz.getDeclaredMethods()));
        }
        return declaredMethods;
    }

    private static BooleanSupplier and(BooleanSupplier a, BooleanSupplier b) {
        return () -> a.getAsBoolean() && b.getAsBoolean();
    }

    private static BooleanSupplier not(BooleanSupplier x) {
        return () -> !x.getAsBoolean();
    }

    static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
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
