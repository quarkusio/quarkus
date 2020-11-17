package io.quarkus.deployment;

import static io.quarkus.deployment.util.ReflectUtil.isBuildProducerOf;
import static io.quarkus.deployment.util.ReflectUtil.isConsumerOf;
import static io.quarkus.deployment.util.ReflectUtil.isListOf;
import static io.quarkus.deployment.util.ReflectUtil.isOptionalOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeExtends;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOfParameter;
import static io.quarkus.qlue.ConsumeFlag.OPTIONAL;
import static io.quarkus.qlue.ProduceFlag.OVERRIDABLE;
import static io.quarkus.qlue.ProduceFlag.WEAK;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.LegacyEmptyItem;
import io.quarkus.builder.LegacyMultiItem;
import io.quarkus.builder.LegacySimpleItem;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
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
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qlue.AttachmentKey;
import io.quarkus.qlue.InjectionMapper;
import io.quarkus.qlue.ProduceFlags;
import io.quarkus.qlue.StepBuilder;
import io.quarkus.qlue.StepContext;
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

    private static final Logger cfgLog = Logger.getLogger("io.quarkus.configuration");
    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    private static boolean isRecorder(AnnotatedElement element) {
        return element.isAnnotationPresent(Recorder.class);
    }

    /**
     * Load all the build steps from the given class loader.
     *
     * @param classLoader the class loader
     * @return a consumer which adds the steps to the given chain builder
     * @throws IOException if the class loader could not load a resource
     * @throws ClassNotFoundException if a build step class is not found
     */
    @Deprecated
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
    @Deprecated
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, LaunchMode launchMode,
            Consumer<ConfigBuilder> configCustomizer)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, new Properties(), launchMode, configCustomizer);
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
    @Deprecated
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, Properties buildSystemProps)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, buildSystemProps, LaunchMode.NORMAL, null);
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
    @Deprecated
    public static Consumer<BuildChainBuilder> loadStepsFrom(ClassLoader classLoader, Properties buildSystemProps,
            LaunchMode launchMode, Consumer<ConfigBuilder> configCustomizer)
            throws IOException, ClassNotFoundException {
        return loadStepsFrom(classLoader, buildSystemProps, Collections.emptyMap(), launchMode, configCustomizer);
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
            Map<String, String> platformProperties, LaunchMode launchMode, Consumer<ConfigBuilder> configCustomizer)
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
        final SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false);

        final DefaultValuesConfigurationSource ds1 = new DefaultValuesConfigurationSource(
                reader.getBuildTimePatternMap());
        final DefaultValuesConfigurationSource ds2 = new DefaultValuesConfigurationSource(
                reader.getBuildTimeRunTimePatternMap());
        final PropertiesConfigSource pcs = new PropertiesConfigSource(buildSystemProps, "Build system");
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

        // the proxy objects used for run time config in the recorders
        Map<Class<?>, Object> proxies = new HashMap<>();
        Consumer<BuildChainBuilder> result = bcb -> bcb.getChainBuilder()
                .setInjectionMapper(new InjectionMapperImpl(proxies, launchMode, readResult));
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, "META-INF/quarkus-build-steps.list")) {
            try {
                result = result.andThen(bcb -> bcb.getChainBuilder().addStepClass(clazz));
            } catch (Throwable e) {
                throw new RuntimeException("Failed to load steps from " + clazz, e);
            }
        }
        result = result.andThen(bcb -> {
            // this has to be an identity hash map else the recorder will get angry
            Map<Object, FieldDescriptor> proxyFields = new IdentityHashMap<>();
            for (Map.Entry<Class<?>, Object> entry : proxies.entrySet()) {
                final RootDefinition def = readResult.requireRootDefinitionForClass(entry.getKey());
                proxyFields.put(entry.getValue(), def.getDescriptor());
            }
            bcb.getChainBuilder().addStepObject(new Object() {
                @BuildStep
                public ConfigurationBuildItem produceConfigurationBuildItem() {
                    return new ConfigurationBuildItem(readResult);
                }

                @BuildStep
                public RunTimeConfigurationProxyBuildItem produceRunTimeConfigurationProxyBuildItem() {
                    return new RunTimeConfigurationProxyBuildItem(proxies);
                }

                @BuildStep
                public BytecodeRecorderObjectLoaderBuildItem produceBytecodeRecorderObjectLoaderBuildItem() {
                    final ObjectLoader loader = new ObjectLoader() {
                        public ResultHandle load(final BytecodeCreator body, final Object obj, final boolean staticInit) {
                            return body.readStaticField(proxyFields.get(obj));
                        }

                        public boolean canHandleObject(final Object obj, final boolean staticInit) {
                            return proxyFields.containsKey(obj);
                        }
                    };
                    return new BytecodeRecorderObjectLoaderBuildItem(loader);
                }
            });
        });
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
    @SuppressWarnings("unused")
    @Deprecated
    public static Consumer<BuildChainBuilder> loadStepsFrom(Class<?> clazz, BuildTimeConfigurationReader.ReadResult readResult,
            Map<Class<?>, Object> runTimeProxies, final LaunchMode launchMode) {
        return bcb -> bcb.getChainBuilder().addStepClass(clazz);
    }

    protected static List<Method> getMethods(Class<?> clazz) {
        List<Method> declaredMethods = new ArrayList<>();
        if (!clazz.getName().equals(Object.class.getName())) {
            declaredMethods.addAll(getMethods(clazz.getSuperclass()));
            declaredMethods.addAll(asList(clazz.getDeclaredMethods()));
        }
        return declaredMethods;
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

    /**
     * The injection mapper that wires the Quarkus-specific build step annotations and features.
     */
    static class InjectionMapperImpl implements InjectionMapper.Delegating {
        private static final AttachmentKey<BytecodeRecorderImpl> BRI_KEY = new AttachmentKey<>();
        private static final AttachmentKey<EnumSet<ConfigPhase>> CCP_KEY = new AttachmentKey<>();

        final Map<Class<?>, Object> runTimeProxies;
        final LaunchMode launchMode;
        final BuildTimeConfigurationReader.ReadResult readResult;
        final Map<Class<? extends BooleanSupplier>, BooleanSupplier> condCache = new HashMap<>();

        InjectionMapperImpl(final Map<Class<?>, Object> runTimeProxies, final LaunchMode launchMode,
                final BuildTimeConfigurationReader.ReadResult readResult) {
            this.runTimeProxies = runTimeProxies;
            this.launchMode = launchMode;
            this.readResult = readResult;
        }

        public InjectionMapper getDelegate() {
            return BASIC;
        }

        public boolean isStepMethod(final Method method) {
            BuildStep step = method.getAnnotation(BuildStep.class);
            if (step == null) {
                return getDelegate().isStepMethod(method);
            }
            Class<? extends BooleanSupplier>[] when = step.onlyIf();
            for (Class<? extends BooleanSupplier> clazz : when) {
                if (!getCondClass(clazz).getAsBoolean()) {
                    return false;
                }
            }
            Class<? extends BooleanSupplier>[] unless = step.onlyIfNot();
            for (Class<? extends BooleanSupplier> clazz : unless) {
                if (getCondClass(clazz).getAsBoolean()) {
                    return false;
                }
            }
            return true;
        }

        public Consumer<StepContext> handleClass(final StepBuilder stepBuilder, final Class<?> clazz)
                throws IllegalArgumentException {
            stepBuilder.putAttachment(CCP_KEY, EnumSet.noneOf(ConfigPhase.class));
            return getDelegate().handleClass(stepBuilder, clazz);
        }

        public Consumer<StepContext> handleClassFinish(final StepBuilder stepBuilder, final Class<?> clazz)
                throws IllegalArgumentException {
            // Process consumed config phases
            if (stepBuilder.getAttachment(CCP_KEY).contains(ConfigPhase.BOOTSTRAP)) {
                stepBuilder.afterProduce(LegacyEmptyItem.class, BootstrapConfigSetupCompleteBuildItem.class);
            }
            if (stepBuilder.getAttachment(CCP_KEY).contains(ConfigPhase.RUN_TIME)) {
                stepBuilder.afterProduce(LegacyEmptyItem.class, RuntimeConfigSetupCompleteBuildItem.class);
            }
            return getDelegate().handleClassFinish(stepBuilder, clazz);
        }

        public Function<StepContext, Object> handleParameter(final StepBuilder stepBuilder, final Executable executable,
                final int paramIndex) throws IllegalArgumentException {
            Parameter parameter = executable.getParameters()[paramIndex];
            Class<?> parameterClass = parameter.getType();
            final Record recordAnnotation = executable.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            Type parameterType = parameter.getParameterizedType();
            final boolean weak = parameter.isAnnotationPresent(Weak.class);
            final boolean overridable = parameter.isAnnotationPresent(Overridable.class);
            // Consume simple
            if (rawTypeExtends(parameterType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = parameterClass
                        .asSubclass(SimpleBuildItem.class);
                stepBuilder.consumes(LegacySimpleItem.class, buildItemClass);
                return sc -> sc.consume(LegacySimpleItem.class, buildItemClass).getItem();
            }
            // Consume multi
            if (isListOf(parameterType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                        .asSubclass(MultiBuildItem.class);
                stepBuilder.consumes(LegacyMultiItem.class, buildItemClass);
                return sc -> {
                    List<LegacyMultiItem> wrappers = sc.consumeMulti(LegacyMultiItem.class, buildItemClass);
                    ArrayList<MultiBuildItem> realItems = new ArrayList<>(wrappers.size());
                    for (LegacyMultiItem wrapper : wrappers) {
                        realItems.add(wrapper.getItem());
                    }
                    return realItems;
                };
            }
            // Produce
            if (isBuildProducerOf(parameterType, BuildItem.class) || isConsumerOf(parameterType, BuildItem.class)) {
                if (executable instanceof Constructor) {
                    throw reportError(parameter, "Cannot produce from constructor");
                }
                final Class<? extends BuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                        .asSubclass(BuildItem.class);
                boolean multi = MultiBuildItem.class.isAssignableFrom(buildItemClass);
                ProduceFlags flags = ProduceFlags.NONE;
                boolean consumer = isConsumerOf(parameterType, BuildItem.class);
                if (overridable) {
                    flags = flags.with(OVERRIDABLE);
                }
                if (weak) {
                    flags = flags.with(WEAK);
                }
                if (multi) {
                    final Class<? extends MultiBuildItem> multiItemClass = buildItemClass.asSubclass(MultiBuildItem.class);
                    stepBuilder.produces(LegacyMultiItem.class, multiItemClass, flags);
                    if (consumer) {
                        return sc -> new Consumer<MultiBuildItem>() {
                            public void accept(final MultiBuildItem o) {
                                sc.produce(LegacyMultiItem.class, multiItemClass, new LegacyMultiItem(multiItemClass.cast(o)));
                            }
                        };
                    } else {
                        return sc -> new BuildProducer<MultiBuildItem>() {
                            public void produce(final MultiBuildItem o) {
                                sc.produce(LegacyMultiItem.class, multiItemClass, new LegacyMultiItem(multiItemClass.cast(o)));
                            }
                        };
                    }
                } else {
                    final Class<? extends SimpleBuildItem> simpleItemClass = buildItemClass.asSubclass(SimpleBuildItem.class);
                    stepBuilder.produces(LegacySimpleItem.class, simpleItemClass, flags);
                    if (consumer) {
                        return sc -> new Consumer<SimpleBuildItem>() {
                            public void accept(final SimpleBuildItem simpleBuildItem) {
                                sc.produce(LegacySimpleItem.class, simpleItemClass,
                                        new LegacySimpleItem(simpleItemClass.cast(simpleBuildItem)));
                            }
                        };
                    } else {
                        return sc -> new BuildProducer<SimpleBuildItem>() {
                            public void produce(final SimpleBuildItem simpleBuildItem) {
                                sc.produce(LegacySimpleItem.class, simpleItemClass,
                                        new LegacySimpleItem(simpleItemClass.cast(simpleBuildItem)));
                            }
                        };
                    }
                }
            }
            // Launch mode
            if (parameterClass == LaunchMode.class) {
                return sc -> launchMode;
            }
            // Consume simple optional
            if (isOptionalOf(parameterType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(parameterType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepBuilder.consumes(LegacySimpleItem.class, buildItemClass, OPTIONAL);
                return sc -> Optional.ofNullable(sc.consume(LegacySimpleItem.class, buildItemClass))
                        .map(LegacySimpleItem::getItem);
            }
            // Consume Executor
            if (parameterClass == Executor.class) {
                return StepContext::getExecutor;
            }
            // Consume recorder proxy
            if (isRecorder(parameter.getType())) {
                if (executable instanceof Constructor) {
                    throw reportError(parameter, "Constructors cannot use recorders");
                }
                if (!isRecorder) {
                    throw reportError(parameter,
                            "Cannot pass recorders to method which is not annotated with " + Record.class);
                }
                return sc -> sc.getAttachment(BRI_KEY).getRecordingProxy(parameterClass);
            }
            // Consume recorder context
            if (parameter.getType() == RecorderContext.class || parameter.getType() == BytecodeRecorderImpl.class) {
                if (!isRecorder) {
                    throw reportError(parameter,
                            "Cannot pass recorder context to method which is not annotated with " + Record.class);
                }
                return sc -> sc.getAttachment(BRI_KEY);
            }
            // Consume Configuration
            if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                final ConfigPhase phase = annotation.phase();
                stepBuilder.getAttachment(CCP_KEY).add(phase);

                if (phase.isAvailableAtBuild()) {
                    if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        runTimeProxies.computeIfAbsent(parameterClass, readResult::requireRootObjectForClass);
                    }
                    stepBuilder.consumes(LegacySimpleItem.class, ConfigurationBuildItem.class);
                    return sc -> {
                        ConfigurationBuildItem cbi = (ConfigurationBuildItem) sc
                                .consume(LegacySimpleItem.class, ConfigurationBuildItem.class).getItem();
                        return cbi.getReadResult().requireRootObjectForClass(parameterClass);
                    };
                } else if (phase.isReadAtMain()) {
                    if (isRecorder) {
                        runTimeProxies.computeIfAbsent(parameterClass, ReflectUtil::newInstance);
                        stepBuilder.consumes(LegacySimpleItem.class, RunTimeConfigurationProxyBuildItem.class);
                        return sc -> {
                            RunTimeConfigurationProxyBuildItem proxies = (RunTimeConfigurationProxyBuildItem) sc
                                    .consume(LegacySimpleItem.class, RunTimeConfigurationProxyBuildItem.class).getItem();
                            return proxies.getProxyObjectFor(parameterClass);
                        };
                    } else {
                        throw reportError(parameter,
                                phase + " configuration cannot be consumed here unless the method is a @Recorder");
                    }
                } else {
                    throw reportError(parameterClass, "Unknown value for ConfigPhase");
                }
            }
            // Unknown
            return getDelegate().handleParameter(stepBuilder, executable, paramIndex);
        }

        public Function<StepContext, Object> handleField(final StepBuilder stepBuilder, final Field field)
                throws IllegalArgumentException {
            Type fieldType = field.getGenericType();
            Class<?> fieldClass = field.getType();
            // Consume simple
            if (rawTypeExtends(fieldType, SimpleBuildItem.class)) {
                Class<? extends SimpleBuildItem> buildItemClass = fieldClass.asSubclass(SimpleBuildItem.class);
                stepBuilder.consumes(LegacySimpleItem.class, buildItemClass);
                return sc -> sc.consume(LegacySimpleItem.class, buildItemClass).getItem();
            }
            // Consume multi
            if (isListOf(fieldType, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(MultiBuildItem.class);
                stepBuilder.consumes(LegacyMultiItem.class, buildItemClass);
                return sc -> {
                    List<LegacyMultiItem> wrappers = sc.consumeMulti(LegacyMultiItem.class, buildItemClass);
                    ArrayList<MultiBuildItem> realItems = new ArrayList<>(wrappers.size());
                    for (LegacyMultiItem wrapper : wrappers) {
                        realItems.add(wrapper.getItem());
                    }
                    return realItems;
                };
            }
            // build producer
            if (isConsumerOf(fieldType, BuildItem.class) || isBuildProducerOf(fieldType, BuildItem.class)) {
                throw reportError(field, "Producers are no longer supported in fields");
            }
            // Consume simple optional
            if (isOptionalOf(fieldType, SimpleBuildItem.class)) {
                final Class<? extends SimpleBuildItem> buildItemClass = rawTypeOfParameter(fieldType, 0)
                        .asSubclass(SimpleBuildItem.class);
                stepBuilder.consumes(LegacySimpleItem.class, buildItemClass, OPTIONAL);
                return sc -> Optional.ofNullable(sc.consume(LegacySimpleItem.class, buildItemClass))
                        .map(LegacySimpleItem::getItem);
            }
            // Launch mode
            if (fieldClass == LaunchMode.class) {
                return sc -> launchMode;
            }
            // Consume Executor
            if (fieldClass == Executor.class) {
                return StepContext::getExecutor;
            }
            // Consume Configuration
            if (fieldClass.isAnnotationPresent(ConfigRoot.class)) {
                final ConfigRoot annotation = fieldClass.getAnnotation(ConfigRoot.class);
                final ConfigPhase phase = annotation.phase();
                stepBuilder.getAttachment(CCP_KEY).add(phase);

                if (phase.isAvailableAtBuild()) {
                    if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                        runTimeProxies.computeIfAbsent(fieldClass, readResult::requireRootObjectForClass);
                    }
                    stepBuilder.consumes(LegacySimpleItem.class, ConfigurationBuildItem.class);
                    return sc -> {
                        ConfigurationBuildItem cbi = (ConfigurationBuildItem) sc
                                .consume(LegacySimpleItem.class, ConfigurationBuildItem.class).getItem();
                        return cbi.getReadResult().requireRootObjectForClass(fieldClass);
                    };
                } else if (phase.isReadAtMain()) {
                    throw reportError(field, phase + " configuration cannot be consumed here");
                } else {
                    throw reportError(fieldClass, "Unknown value for ConfigPhase");
                }
            }
            // Consume recorder
            if (isRecorder(fieldClass)) {
                throw reportError(field, "Bytecode recorders disallowed on fields");
            }
            // Unknown
            return getDelegate().handleField(stepBuilder, field);
        }

        public Consumer<StepContext> handleStepMethod(final StepBuilder stepBuilder, final Method method)
                throws IllegalArgumentException {
            stepBuilder.putAttachment(CCP_KEY, EnumSet.noneOf(ConfigPhase.class));
            final Record recordAnnotation = method.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            Consume[] consumes = method.getAnnotationsByType(Consume.class);
            for (Consume consume : consumes) {
                Class<? extends BuildItem> itemType = consume.value();
                if (SimpleBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.afterProduce(LegacySimpleItem.class, itemType.asSubclass(SimpleBuildItem.class));
                } else if (MultiBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.afterProduce(LegacyMultiItem.class, itemType.asSubclass(MultiBuildItem.class));
                } else if (EmptyBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.afterProduce(LegacyEmptyItem.class, itemType.asSubclass(EmptyBuildItem.class));
                } else {
                    throw reportError(method, "Method consumes invalid build item type");
                }
            }
            Produce[] produces = method.getAnnotationsByType(Produce.class);
            for (Produce produce : produces) {
                Class<? extends BuildItem> itemType = produce.value();
                if (SimpleBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacySimpleItem.class, itemType.asSubclass(SimpleBuildItem.class));
                } else if (MultiBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacyMultiItem.class, itemType.asSubclass(MultiBuildItem.class));
                } else if (EmptyBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacyEmptyItem.class, itemType.asSubclass(EmptyBuildItem.class));
                } else {
                    throw reportError(method, "Method consumes invalid build item type");
                }
            }
            ProduceWeak[] producesWeak = method.getAnnotationsByType(ProduceWeak.class);
            for (ProduceWeak produceWeak : producesWeak) {
                Class<? extends BuildItem> itemType = produceWeak.value();
                if (SimpleBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacySimpleItem.class, itemType.asSubclass(SimpleBuildItem.class), WEAK);
                } else if (MultiBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacyMultiItem.class, itemType.asSubclass(MultiBuildItem.class), WEAK);
                } else if (EmptyBuildItem.class.isAssignableFrom(itemType)) {
                    stepBuilder.beforeConsume(LegacyEmptyItem.class, itemType.asSubclass(EmptyBuildItem.class), WEAK);
                } else {
                    throw reportError(method, "Method consumes invalid build item type");
                }
            }
            if (isRecorder) {
                boolean recorderFound = false;
                for (Class<?> p : method.getParameterTypes()) {
                    if (isRecorder(p)) {
                        recorderFound = true;
                        break;
                    }
                }
                if (!recorderFound) {
                    throw reportError(method, "Method is marked @Record but does not inject an @Recorder object");
                }
                ProduceFlags flags = ProduceFlags.NONE;
                if (recordAnnotation.optional()) {
                    flags = flags.with(WEAK);
                }
                boolean staticInit = recordAnnotation.value() == ExecutionTime.STATIC_INIT;
                if (staticInit) {
                    stepBuilder.produces(LegacyMultiItem.class, StaticBytecodeRecorderBuildItem.class, flags);
                } else {
                    stepBuilder.produces(LegacyMultiItem.class, MainBytecodeRecorderBuildItem.class, flags);
                }
                return sc -> sc.putAttachment(BRI_KEY, new BytecodeRecorderImpl(
                        staticInit,
                        method.getDeclaringClass().getName(),
                        method.getName(),
                        Integer.toString(method.toString().hashCode())));
            } else {
                return sc -> {
                };
            }
        }

        @SuppressWarnings("unchecked")
        public BiConsumer<StepContext, Object> handleReturnValue(final StepBuilder stepBuilder, final Method method)
                throws IllegalArgumentException {
            Type type = method.getGenericReturnType();
            Class<?> clazz = method.getReturnType();
            ProduceFlags flags = ProduceFlags.NONE;
            if (method.getAnnotation(Overridable.class) != null) {
                flags = flags.with(OVERRIDABLE);
            }
            if (method.getAnnotation(Weak.class) != null) {
                flags = flags.with(WEAK);
            }
            final Record recordAnnotation = method.getAnnotation(Record.class);
            final boolean isRecorder = recordAnnotation != null;
            boolean staticInit = isRecorder && recordAnnotation.value() == ExecutionTime.STATIC_INIT;
            BiConsumer<StepContext, Object> delegate;
            if (isRecorder) {
                // commit the result
                if (staticInit) {
                    delegate = (sc, v) -> sc.produce(LegacyMultiItem.class, StaticBytecodeRecorderBuildItem.class,
                            new LegacyMultiItem(new StaticBytecodeRecorderBuildItem(sc.getAttachment(BRI_KEY))));
                } else {
                    delegate = (sc, v) -> sc.produce(LegacyMultiItem.class, MainBytecodeRecorderBuildItem.class,
                            new LegacyMultiItem(new MainBytecodeRecorderBuildItem(sc.getAttachment(BRI_KEY))));
                }
            } else {
                delegate = (sc, v) -> {
                };
            }
            // Process consumed config phases
            if (stepBuilder.getAttachment(CCP_KEY).contains(ConfigPhase.BOOTSTRAP)) {
                if (isRecorder && staticInit) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }
                stepBuilder.afterProduce(LegacyEmptyItem.class, BootstrapConfigSetupCompleteBuildItem.class);
                stepBuilder.afterProduce(LegacySimpleItem.class, RunTimeConfigurationProxyBuildItem.class);
            }
            if (stepBuilder.getAttachment(CCP_KEY).contains(ConfigPhase.RUN_TIME)) {
                if (isRecorder && staticInit) {
                    throw reportError(method,
                            "Bytecode recorder is static but an injected config object is declared as run time");
                }
                stepBuilder.afterProduce(LegacyEmptyItem.class, RuntimeConfigSetupCompleteBuildItem.class);
                stepBuilder.afterProduce(LegacySimpleItem.class, RunTimeConfigurationProxyBuildItem.class);
            }

            // Produce simple
            if (SimpleBuildItem.class.isAssignableFrom(clazz)) {
                final Class<? extends SimpleBuildItem> buildItemClass = clazz
                        .asSubclass(SimpleBuildItem.class);
                stepBuilder.produces(LegacySimpleItem.class, buildItemClass, flags);
                return delegate.andThen((sc, v) -> {
                    if (v != null) {
                        sc.produce(LegacySimpleItem.class, buildItemClass,
                                new LegacySimpleItem(buildItemClass.cast(v)));
                    }
                });
            }
            // Produce multi
            if (rawTypeExtends(type, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = clazz
                        .asSubclass(MultiBuildItem.class);
                stepBuilder.produces(LegacyMultiItem.class, buildItemClass, flags);
                return delegate.andThen((sc, v) -> {
                    if (v != null) {
                        sc.produce(LegacyMultiItem.class, buildItemClass,
                                new LegacyMultiItem(buildItemClass.cast(v)));
                    }
                });
            }
            // Produce multi as list
            if (isListOf(type, MultiBuildItem.class)) {
                final Class<? extends MultiBuildItem> buildItemClass = rawTypeOfParameter(type, 0)
                        .asSubclass(MultiBuildItem.class);
                stepBuilder.produces(LegacyMultiItem.class, buildItemClass, flags);
                return delegate.andThen((sc, v) -> {
                    if (v != null) {
                        List<MultiBuildItem> items = (List<MultiBuildItem>) v;
                        for (MultiBuildItem item : items) {
                            if (item != null) {
                                sc.produce(LegacyMultiItem.class, buildItemClass,
                                        new LegacyMultiItem(buildItemClass.cast(item)));
                            }
                        }
                    }
                });
            }
            // Unknown
            return delegate.andThen(getDelegate().handleReturnValue(stepBuilder, method));
        }

        BooleanSupplier getCondClass(Class<? extends BooleanSupplier> testClass) {
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
                        if (phase.isAvailableAtBuild()) {
                            paramSuppList.add(() -> readResult.requireRootObjectForClass(parameterClass));
                        } else if (phase.isReadAtMain()) {
                            throw reportError(parameter, phase + " configuration cannot be consumed here");
                        } else {
                            throw reportError(parameter,
                                    "Unsupported conditional class configuration build phase " + phase);
                        }
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
                        if (phase.isAvailableAtBuild()) {
                            setup = setup.andThen(o -> ReflectUtil.setFieldVal(field, o,
                                    readResult.requireRootObjectForClass(fieldClass)));
                        } else if (phase.isReadAtMain()) {
                            throw reportError(field, phase + " configuration cannot be consumed here");
                        } else {
                            throw reportError(field,
                                    "Unsupported conditional class configuration build phase " + phase);
                        }
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
            return bs;
        }
    }
}
