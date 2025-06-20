package io.quarkus.deployment;

import static io.quarkus.deployment.ExtensionLoaderConfig.ReportRuntimeConfigAtDeployment.warn;
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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.wildfly.common.function.Functions;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.ConsumeFlag;
import io.quarkus.builder.ConsumeFlags;
import io.quarkus.builder.ProduceFlag;
import io.quarkus.builder.ProduceFlags;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.ProduceWeak;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.Weak;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationProxyBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.ConfigMappingUtils;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.recording.ObjectLoader;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfig;

/**
 * Utility class to load build steps, runtime recorders, and configuration roots from a given extension class.
 */
public final class ExtensionLoader {

    private ExtensionLoader() {
    }

    private static final Logger loadLog = Logger.getLogger("io.quarkus.deployment");
    @SuppressWarnings("unchecked")
    private static final Class<? extends BooleanSupplier>[] EMPTY_BOOLEAN_SUPPLIER_CLASS_ARRAY = new Class[0];

    @SuppressWarnings("deprecation")
    private static boolean isRecorder(AnnotatedElement element) {
        return element.isAnnotationPresent(Recorder.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @param buildSystemProps the build system properties to use
     * @param launchMode launch mode
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader,
            Properties buildSystemProps, Properties runtimeProperties,
            ApplicationModel appModel, LaunchMode launchMode, DevModeType devModeType)
            throws IOException, ClassNotFoundException {

        final BuildTimeConfigurationReader reader = new BuildTimeConfigurationReader(classLoader);
        final SmallRyeConfig src = reader.initConfiguration(launchMode, buildSystemProps, runtimeProperties,
                appModel.getPlatformProperties());
        // install globally
        QuarkusConfigFactory.setConfig(src);
        final BuildTimeConfigurationReader.ReadResult readResult = reader.readConfiguration(src);
        final BooleanSupplierFactoryBuildItem bsf = new BooleanSupplierFactoryBuildItem(readResult, launchMode, devModeType);

        Consumer<BuildChainBuilder> result = Functions.discardingConsumer();
        // BooleanSupplier factory
        result = result.andThen(bcb -> bcb.addBuildStep(new io.quarkus.builder.BuildStep() {

            @Override
            public void execute(BuildContext context) {
                context.produce(bsf);
            }

            @Override
            public String getId() {
                return ExtensionLoader.class.getName() + "#booleanSupplierFactory";
            }
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
        Map<Object, FieldDescriptor> rootFields = new IdentityHashMap<>();
        Map<Object, ConfigClass> mappingClasses = new IdentityHashMap<>();
        for (Map.Entry<Class<?>, Object> entry : proxies.entrySet()) {
            // ConfigRoot
            RootDefinition root = readResult.getAllRootsByClass().get(entry.getKey());
            if (root != null) {
                rootFields.put(entry.getValue(), root.getDescriptor());
                continue;
            }

            // ConfigMapping
            ConfigClass mapping = readResult.getAllMappingsByClass().get(entry.getKey());
            if (mapping != null) {
                mappingClasses.put(entry.getValue(), mapping);
                continue;
            }

            throw new IllegalStateException("No config found for " + entry.getKey());
        }
        result = result.andThen(bcb -> bcb.addBuildStep(new io.quarkus.builder.BuildStep() {

            @Override
            public void execute(BuildContext bc) {
                bc.produce(new ConfigurationBuildItem(readResult));
                bc.produce(new RunTimeConfigurationProxyBuildItem(proxies));

                ObjectLoader rootLoader = new ObjectLoader() {
                    public ResultHandle load(final BytecodeCreator body, final Object obj, final boolean staticInit) {
                        return body.readStaticField(rootFields.get(obj));
                    }

                    public boolean canHandleObject(final Object obj, final boolean staticInit) {
                        return rootFields.containsKey(obj);
                    }
                };

                // Load @ConfigMapping in recorded deployment code from Recorder
                ObjectLoader mappingLoader = new ObjectLoader() {
                    @Override
                    public ResultHandle load(final BytecodeCreator body, final Object obj, final boolean staticInit) {
                        ConfigClass mapping = mappingClasses.get(obj);
                        MethodDescriptor getConfig = MethodDescriptor.ofMethod(ConfigProvider.class, "getConfig", Config.class);
                        ResultHandle config = body.invokeStaticMethod(getConfig);
                        MethodDescriptor getMapping = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getConfigMapping",
                                Object.class, Class.class, String.class);
                        return body.invokeVirtualMethod(getMapping, config, body.loadClass(mapping.getKlass()),
                                body.load(mapping.getPrefix()));
                    }

                    @Override
                    public boolean canHandleObject(final Object obj, final boolean staticInit) {
                        return mappingClasses.containsKey(obj);
                    }
                };

                bc.produce(new BytecodeRecorderObjectLoaderBuildItem(rootLoader));
                bc.produce(new BytecodeRecorderObjectLoaderBuildItem(mappingLoader));
            }

            @Override
            public String getId() {
                return ExtensionLoader.class.getName() + "#config";
            }

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

        ExtensionLoaderConfig extensionLoaderConfig = (ExtensionLoaderConfig) readResult.getObjectsByClass()
                .get(ExtensionLoaderConfig.class);
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
                if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                    final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(parameterType)
                            .asSubclass(SimpleBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consume(buildItemClass));
                } else if (isAnEmptyBuildItemConsumer(parameterType)) {
                    throw reportError(parameter,
                            "Cannot consume an empty build item, use @Consume(class) on the constructor instead");
                } else if (isAnEmptyBuildItemProducer(parameterType)) {
                    throw reportError(parameter,
                            "Cannot produce an empty build item, use @Produce(class) on the constructor instead");
                } else if (isListOf(parameterType, MultiBuildItem.class)) {
                    final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                            .asSubclass(MultiBuildItem.class);
                    stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                    ctorParamFns.add(bc -> bc.consumeMulti(buildItemClass));
                } else if (isConsumerOf(parameterType, BuildItem.class)
                        || isBuildProducerOf(parameterType, BuildItem.class)) {
                    throw unsupportedConstructorOrFieldProducer(parameter);
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
                                .requireObjectForClass(parameterClass));
                        if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                            runTimeProxies.computeIfAbsent(parameterClass, readResult::requireObjectForClass);
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
            if (rawTypeExtends(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOf(fieldType).asSubclass(SimpleBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consume(buildItemClass)));
            } else if (isAnEmptyBuildItemConsumer(fieldType)) {
                throw reportError(field, "Cannot consume an empty build item, use @Consume(class) on the field instead");
            } else if (isAnEmptyBuildItemProducer(fieldType)) {
                throw reportError(field, "Cannot produce an empty build item, use @Produce(class) on the field instead");
            } else if (isListOf(fieldType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(MultiBuildItem.class);
                stepConfig = stepConfig.andThen(bsb -> bsb.consumes(buildItemClass));
                stepInstanceSetup = stepInstanceSetup
                        .andThen((bc, o) -> ReflectUtil.setFieldVal(field, o, bc.consumeMulti(buildItemClass)));
            } else if (isConsumerOf(fieldType, BuildItem.class)
                    || isBuildProducerOf(fieldType, BuildItem.class)) {
                throw unsupportedConstructorOrFieldProducer(field);
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
                                configurationBuildItem.getReadResult().requireObjectForClass(fieldClass));
                    });
                    if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        runTimeProxies.computeIfAbsent(fieldClass, readResult::requireObjectForClass);
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

        // get class-level configuration, if any
        final BuildSteps buildSteps = clazz.getAnnotation(BuildSteps.class);
        final Class<? extends BooleanSupplier>[] classOnlyIf = buildSteps == null ? EMPTY_BOOLEAN_SUPPLIER_CLASS_ARRAY
                : buildSteps.onlyIf();
        final Class<? extends BooleanSupplier>[] classOnlyIfNot = buildSteps == null ? EMPTY_BOOLEAN_SUPPLIER_CLASS_ARRAY
                : buildSteps.onlyIfNot();

        // now iterate the methods
        final List<Method> methods = getMethods(clazz);
        final Map<String, List<Method>> nameToMethods = methods.stream().collect(Collectors.groupingBy(m -> m.getName()));

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
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
            addStep = and(addStep, supplierFactory, classOnlyIf, false);
            addStep = and(addStep, supplierFactory, classOnlyIfNot, true);
            addStep = and(addStep, supplierFactory, onlyIf, false);
            addStep = and(addStep, supplierFactory, onlyIfNot, true);
            final BooleanSupplier finalAddStep = addStep;

            if (isRecorder) {
                assert recordAnnotation != null;
                final ExecutionTime executionTime = recordAnnotation.value();
                final boolean optional = recordAnnotation.optional();
                methodStepConfig = methodStepConfig.andThen(bsb -> {
                    bsb
                            .produces(
                                    executionTime == ExecutionTime.STATIC_INIT ? StaticBytecodeRecorderBuildItem.class
                                            : MainBytecodeRecorderBuildItem.class,
                                    optional ? ProduceFlags.of(ProduceFlag.WEAK) : ProduceFlags.NONE);
                });
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
                    } else if (isAnEmptyBuildItemConsumer(parameterType)) {
                        throw reportError(parameter,
                                "Cannot consume an empty build item, use @Consume(class) on the build step method instead");
                    } else if (isAnEmptyBuildItemProducer(parameterType)) {
                        throw reportError(parameter,
                                "Cannot produce an empty build item, use @Produce(class) on the build step method instead");
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
                    } else if (rawTypeOf(parameterType) == Executor.class
                            || rawTypeOf(parameterType) == ExecutorService.class) {
                        methodParamFns.add((bc, bri) -> bc.getExecutor());
                    } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                        final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                        final ConfigPhase phase = annotation.phase();
                        methodConsumingConfigPhases.add(phase);

                        if (phase.isAvailableAtBuild()) {
                            methodParamFns.add((bc, bri) -> {
                                final ConfigurationBuildItem configurationBuildItem = bc
                                        .consume(ConfigurationBuildItem.class);
                                return configurationBuildItem.getReadResult().requireObjectForClass(parameterClass);
                            });
                            if (isRecorder && phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                                runTimeProxies.computeIfAbsent(parameterClass, readResult::requireObjectForClass);
                            }
                        } else if (phase.isReadAtMain()) {
                            if (isRecorder) {
                                if (extensionLoaderConfig.reportRuntimeConfigAtDeployment().equals(warn)) {
                                    methodParamFns.add((bc, bri) -> {
                                        RunTimeConfigurationProxyBuildItem proxies = bc
                                                .consume(RunTimeConfigurationProxyBuildItem.class);
                                        return proxies.getProxyObjectFor(parameterClass);
                                    });
                                    loadLog.warn(reportError(parameter,
                                            phase + " configuration should not be consumed in Build Steps, use RuntimeValue<"
                                                    + parameter.getType().getTypeName()
                                                    + "> in a @Recorder constructor instead")
                                            .getMessage());
                                    runTimeProxies.computeIfAbsent(parameterClass, ConfigMappingUtils::newInstance);
                                } else {
                                    throw reportError(parameter,
                                            phase + " configuration cannot be consumed in Build Steps, use RuntimeValue<"
                                                    + parameter.getType().getTypeName()
                                                    + "> in a @Recorder constructor instead");
                                }
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
                        //now look for recorder parameter injection
                        //as we now inject config directly into recorders we need to look at the constructor params
                        Constructor<?>[] ctors = parameter.getType().getDeclaredConstructors();
                        for (var ctor : ctors) {
                            if (ctors.length == 1 || ctor.isAnnotationPresent(Inject.class)) {
                                for (var type : ctor.getGenericParameterTypes()) {
                                    Class<?> theType;
                                    boolean isRuntimeValue = false;
                                    if (type instanceof ParameterizedType) {
                                        ParameterizedType pt = (ParameterizedType) type;
                                        if (pt.getRawType().equals(RuntimeValue.class)) {
                                            theType = (Class<?>) pt.getActualTypeArguments()[0];
                                            isRuntimeValue = true;
                                        } else {
                                            throw new RuntimeException("Unknown recorder constructor parameter: " + type
                                                    + " in recorder " + parameter.getType());
                                        }
                                    } else {
                                        theType = (Class<?>) type;
                                    }
                                    ConfigRoot annotation = theType.getAnnotation(ConfigRoot.class);
                                    if (annotation != null) {
                                        if (recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                                            // TODO - Check for runtime config is done in another place, we may want to make things more consistent. Rewrite once we disallow the injection of runtime objects in build steps
                                            methodConsumingConfigPhases.add(ConfigPhase.BUILD_AND_RUN_TIME_FIXED);
                                        } else {
                                            methodConsumingConfigPhases.add(annotation.phase());
                                            if (annotation.phase().isReadAtMain() && !isRuntimeValue) {
                                                if (extensionLoaderConfig.reportRuntimeConfigAtDeployment().equals(warn)) {
                                                    loadLog.warn(reportError(parameter, annotation.phase() + " configuration "
                                                            + type.getTypeName()
                                                            + " should be injected in a @Recorder constructor as a RuntimeValue<"
                                                            + type.getTypeName() + ">").getMessage());
                                                } else {
                                                    throw reportError(parameter, annotation.phase() + " configuration "
                                                            + type.getTypeName()
                                                            + " can only be injected in a @Recorder constructor as a RuntimeValue<"
                                                            + type.getTypeName() + ">");
                                                }
                                            }
                                        }
                                        if (annotation.phase().isReadAtMain()) {
                                            // TODO - Remove once we disallow the injection of runtime objects in build steps
                                            runTimeProxies.computeIfAbsent(theType, ConfigMappingUtils::newInstance);
                                        } else {
                                            runTimeProxies.computeIfAbsent(theType, readResult::requireObjectForClass);
                                        }
                                    }
                                }
                            }
                        }

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
            } else if (rawTypeExtends(returnType, EmptyBuildItem.class) || isOptionalOf(returnType, EmptyBuildItem.class)) {
                throw reportError(method,
                        "Cannot produce an empty build item, use @Produce(class) on the build step method instead");
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

            if (methodConsumingConfigPhases.contains(ConfigPhase.RUN_TIME)) {
                if (isRecorder && recordAnnotation.value() == ExecutionTime.STATIC_INIT) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }

                methodStepConfig = methodStepConfig
                        .andThen(bsb -> bsb.consumes(RunTimeConfigurationProxyBuildItem.class));

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
            final String stepId;
            List<Method> methodsWithName = nameToMethods.get(method.getName());
            if (methodsWithName.size() > 1) {
                // Append the sha1 of the parameter types to resolve the ambiguity
                stepId = name + "_" + HashUtil.sha1(Arrays.toString(method.getParameterTypes()));
                loadLog.debugf("Build steps with ambiguous name detected: %s, using discriminator suffix for step id: %s", name,
                        stepId);
            } else {
                stepId = name;
            }

            MethodHandle methodHandle = unreflect(method, lookup);
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
                                                Integer.toString(Math.abs(method.toString().hashCode())), identityComparison,
                                                s -> {
                                                    if (s instanceof Class) {
                                                        var cfg = ((Class<?>) s).getAnnotation(ConfigRoot.class);
                                                        if (cfg == null
                                                                || (cfg.phase() != ConfigPhase.BUILD_AND_RUN_TIME_FIXED
                                                                        && recordAnnotation
                                                                                .value() == ExecutionTime.STATIC_INIT)) {
                                                            throw new RuntimeException(
                                                                    "Can only inject BUILD_AND_RUN_TIME_FIXED objects into a constructor, use RuntimeValue to inject runtime config: "
                                                                            + s);
                                                        }
                                                        return runTimeProxies.get(s);
                                                    }
                                                    // TODO - Remove once we disallow the injection of runtime objects in build steps
                                                    if (s instanceof ParameterizedType) {
                                                        ParameterizedType p = (ParameterizedType) s;
                                                        if (p.getRawType() == RuntimeValue.class) {
                                                            Object object = runTimeProxies.get(p.getActualTypeArguments()[0]);
                                                            if (object == null) {
                                                                return new RuntimeValue<>();
                                                            }
                                                            return new RuntimeValue<>(object);
                                                        }
                                                    }
                                                    return null;
                                                })
                                        : null;
                                for (int i = 0; i < methodArgs.length; i++) {
                                    methodArgs[i] = methodParamFns.get(i).apply(bc, bri);
                                }
                                Object result;
                                try {
                                    result = methodHandle.bindTo(instance).invokeWithArguments(methodArgs);
                                } catch (IllegalAccessException e) {
                                    throw ReflectUtil.toError(e);
                                } catch (RuntimeException | Error e2) {
                                    throw e2;
                                } catch (Throwable t) {
                                    throw new UndeclaredThrowableException(t);
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

                            @Override
                            public String getId() {
                                return stepId;
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

    private static MethodHandle unreflect(Method method, MethodHandles.Lookup lookup) {
        try {
            return lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            throw ReflectUtil.toError(e);
        }

    }

    private static BooleanSupplier and(BooleanSupplier addStep, BooleanSupplierFactoryBuildItem supplierFactory,
            Class<? extends BooleanSupplier>[] testClasses, boolean inv) {
        for (Class<? extends BooleanSupplier> testClass : testClasses) {
            BooleanSupplier bs = supplierFactory.get((Class<? extends BooleanSupplier>) testClass);
            if (inv) {
                addStep = and(addStep, not(bs));
            } else {
                addStep = and(addStep, bs);
            }
        }
        return addStep;
    }

    private static boolean isAnEmptyBuildItemProducer(Type parameterType) {
        return isBuildProducerOf(parameterType, EmptyBuildItem.class)
                || isSupplierOf(parameterType, EmptyBuildItem.class)
                || isSupplierOfOptionalOf(parameterType, EmptyBuildItem.class);
    }

    private static boolean isAnEmptyBuildItemConsumer(Type parameterType) {
        return rawTypeExtends(parameterType, EmptyBuildItem.class)
                || isOptionalOf(parameterType, EmptyBuildItem.class)
                || isConsumerOf(parameterType, EmptyBuildItem.class);
    }

    private static IllegalArgumentException unsupportedConstructorOrFieldProducer(final AnnotatedElement element) {
        return reportError(element, "Producing values from constructors or fields is no longer supported."
                + " Inject the BuildProducer/Consumer through arguments of relevant @BuildStep methods instead.");
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
