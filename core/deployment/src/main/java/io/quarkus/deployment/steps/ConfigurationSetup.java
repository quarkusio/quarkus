package io.quarkus.deployment.steps;

import static io.quarkus.deployment.util.ReflectUtil.toError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.objectweb.asm.Opcodes;
import org.wildfly.common.net.CidrAddress;
import org.wildfly.security.ssl.CipherSuiteSelector;
import org.wildfly.security.ssl.Protocol;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.BuildTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.BuildTimeRunTimeFixedConfigurationBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationCustomConverterBuildItem;
import io.quarkus.deployment.builditem.ExtensionClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.configuration.ConfigDefinition;
import io.quarkus.deployment.configuration.ConfigPatternMap;
import io.quarkus.deployment.configuration.LeafConfigType;
import io.quarkus.deployment.recording.ObjectLoader;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ApplicationPropertiesConfigSource;
import io.quarkus.runtime.configuration.BuildTimeConfigFactory;
import io.quarkus.runtime.configuration.CidrAddressConverter;
import io.quarkus.runtime.configuration.ConverterFactory;
import io.quarkus.runtime.configuration.DefaultConfigSource;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.InetAddressConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.runtime.configuration.PathConverter;
import io.quarkus.runtime.configuration.RegexConverter;
import io.quarkus.runtime.configuration.SimpleConfigurationProviderResolver;
import io.quarkus.runtime.configuration.ssl.CipherSuiteSelectorConverter;
import io.quarkus.runtime.configuration.ssl.ProtocolConverter;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Setup steps for configuration purposes.
 */
public class ConfigurationSetup {

    private static final Logger log = Logger.getLogger("io.quarkus.configuration");

    public static final String BUILD_TIME_CONFIG = "io.quarkus.runtime.generated.BuildTimeConfig";
    public static final String BUILD_TIME_CONFIG_ROOT = "io.quarkus.runtime.generated.BuildTimeConfigRoot";
    public static final String RUN_TIME_CONFIG = "io.quarkus.runtime.generated.RunTimeConfig";
    public static final String RUN_TIME_CONFIG_ROOT = "io.quarkus.runtime.generated.RunTimeConfigRoot";

    public static final MethodDescriptor CREATE_RUN_TIME_CONFIG = MethodDescriptor.ofMethod(RUN_TIME_CONFIG,
            "getRunTimeConfiguration", void.class);

    private static final FieldDescriptor RUN_TIME_CONFIG_FIELD = FieldDescriptor.of(RUN_TIME_CONFIG, "runConfig",
            RUN_TIME_CONFIG_ROOT);
    private static final FieldDescriptor BUILD_TIME_CONFIG_FIELD = FieldDescriptor.of(BUILD_TIME_CONFIG, "buildConfig",
            BUILD_TIME_CONFIG_ROOT);
    private static final FieldDescriptor CONVERTERS_FIELD = FieldDescriptor.of(BUILD_TIME_CONFIG, "converters",
            Converter[].class);

    private static final MethodDescriptor NI_HAS_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor NI_NEXT_EQUALS = MethodDescriptor.ofMethod(NameIterator.class, "nextSegmentEquals",
            boolean.class, String.class);
    private static final MethodDescriptor NI_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);
    private static final MethodDescriptor ITR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor ITR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);
    private static final MethodDescriptor CF_GET_IMPLICIT_CONVERTER = MethodDescriptor.ofMethod(ConverterFactory.class,
            "getImplicitConverter", Converter.class, Class.class);
    private static final MethodDescriptor CPR_SET_INSTANCE = MethodDescriptor.ofMethod(ConfigProviderResolver.class,
            "setInstance", void.class, ConfigProviderResolver.class);
    private static final MethodDescriptor SCPR_CONSTRUCT = MethodDescriptor
            .ofConstructor(SimpleConfigurationProviderResolver.class, Config.class);
    private static final MethodDescriptor SRCB_BUILD = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "build",
            Config.class);
    private static final MethodDescriptor SRCB_WITH_CONVERTER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withConverter", ConfigBuilder.class, Class.class, int.class, Converter.class);
    private static final MethodDescriptor SRCB_WITH_SOURCES = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withSources", ConfigBuilder.class, ConfigSource[].class);
    private static final MethodDescriptor SRCB_ADD_DEFAULT_SOURCES = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "addDefaultSources", ConfigBuilder.class);
    private static final MethodDescriptor SRCB_CONSTRUCT = MethodDescriptor.ofConstructor(SmallRyeConfigBuilder.class);
    private static final MethodDescriptor II_IN_IMAGE_RUN = MethodDescriptor.ofMethod(ImageInfo.class, "inImageRuntimeCode",
            boolean.class);
    private static final MethodDescriptor SRCB_WITH_WRAPPER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withWrapper", SmallRyeConfigBuilder.class, UnaryOperator.class);

    private static final MethodDescriptor BTCF_GET_CONFIG_SOURCE = MethodDescriptor.ofMethod(BuildTimeConfigFactory.class,
            "getBuildTimeConfigSource", ConfigSource.class);

    private static final FieldDescriptor ECS_WRAPPER = FieldDescriptor.of(ExpandingConfigSource.class, "WRAPPER",
            UnaryOperator.class);
    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    public ConfigurationSetup() {
    }

    @BuildStep
    public void setUpConverters(BuildProducer<ConfigurationCustomConverterBuildItem> configurationTypes) {
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                InetSocketAddress.class,
                InetSocketAddressConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                CidrAddress.class,
                CidrAddressConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                InetAddress.class,
                InetAddressConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                Pattern.class,
                RegexConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                CipherSuiteSelector.class,
                CipherSuiteSelectorConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                Protocol.class,
                ProtocolConverter.class));
        configurationTypes.produce(new ConfigurationCustomConverterBuildItem(
                200,
                Path.class,
                PathConverter.class));
    }

    /**
     * Run before anything that consumes configuration; sets up the main configuration definition instance.
     *
     * @param converters the converters to set up
     * @param runTimeConfigConsumer the run time config consumer
     * @param buildTimeConfigConsumer the build time config consumer
     * @param buildTimeRunTimeConfigConsumer the build time/run time fixed config consumer
     * @param extensionClassLoaderBuildItem the extension class loader build item
     * @param archiveRootBuildItem the application archive root
     */
    @BuildStep
    public void initializeConfiguration(
            List<ConfigurationCustomConverterBuildItem> converters,
            Consumer<RunTimeConfigurationBuildItem> runTimeConfigConsumer,
            Consumer<BuildTimeConfigurationBuildItem> buildTimeConfigConsumer,
            Consumer<BuildTimeRunTimeFixedConfigurationBuildItem> buildTimeRunTimeConfigConsumer,
            ExtensionClassLoaderBuildItem extensionClassLoaderBuildItem,
            ArchiveRootBuildItem archiveRootBuildItem) throws IOException, ClassNotFoundException {
        // set up the configuration definitions
        final ConfigDefinition buildTimeConfig = new ConfigDefinition(FieldDescriptor.of("Bogus", "No field", "Nothing"));
        final ConfigDefinition buildTimeRunTimeConfig = new ConfigDefinition(BUILD_TIME_CONFIG_FIELD);
        final ConfigDefinition runTimeConfig = new ConfigDefinition(RUN_TIME_CONFIG_FIELD);
        // populate it with all known types
        for (Class<?> clazz : ServiceUtil.classesNamedIn(extensionClassLoaderBuildItem.getExtensionClassLoader(),
                CONFIG_ROOTS_LIST)) {
            final ConfigRoot annotation = clazz.getAnnotation(ConfigRoot.class);
            if (annotation == null) {
                log.warnf("Ignoring configuration root %s because it has no annotation", clazz);
            } else {
                final ConfigPhase phase = annotation.phase();
                if (phase == ConfigPhase.RUN_TIME) {
                    runTimeConfig.registerConfigRoot(clazz);
                } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                    buildTimeRunTimeConfig.registerConfigRoot(clazz);
                } else if (phase == ConfigPhase.BUILD_TIME) {
                    buildTimeConfig.registerConfigRoot(clazz);
                } else {
                    log.warnf("Unrecognized configuration phase \"%s\" on %s", phase, clazz);
                }
            }
        }

        // now prepare & load the build configuration
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        // expand properties
        builder.withWrapper(ExpandingConfigSource::new);
        builder.addDefaultSources();
        builder.withSources(new ApplicationPropertiesConfigSource.InJar());
        for (ConfigurationCustomConverterBuildItem converter : converters) {
            withConverterHelper(builder, converter.getType(), converter.getPriority(), converter.getConverter());
        }
        final SmallRyeConfig src = (SmallRyeConfig) builder.build();
        SmallRyeConfigProviderResolver.instance().registerConfig(src, Thread.currentThread().getContextClassLoader());
        ConfigDefinition.loadConfiguration(src,
                buildTimeConfig,
                buildTimeRunTimeConfig,
                runTimeConfig // this one is only for generating a default-values config source
        );

        // produce the config objects
        runTimeConfigConsumer.accept(new RunTimeConfigurationBuildItem(runTimeConfig));
        buildTimeRunTimeConfigConsumer.accept(new BuildTimeRunTimeFixedConfigurationBuildItem(buildTimeRunTimeConfig));
        buildTimeConfigConsumer.accept(new BuildTimeConfigurationBuildItem(buildTimeConfig));
    }

    @SuppressWarnings("unchecked")
    private static <T> void withConverterHelper(final SmallRyeConfigBuilder builder, final Class<T> type, final int priority,
            final Class<? extends Converter<?>> converterClass) {
        try {
            builder.withConverter(type, priority, ((Class<? extends Converter<T>>) converterClass).newInstance());
        } catch (InstantiationException e) {
            throw toError(e);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    /**
     * Add a config sources for {@code application.properties}.
     */
    @BuildStep
    void setUpConfigFile(BuildProducer<RunTimeConfigurationSourceBuildItem> configSourceConsumer) {
        configSourceConsumer.produce(new RunTimeConfigurationSourceBuildItem(
                ApplicationPropertiesConfigSource.InJar.class.getName(), OptionalInt.empty()));
        configSourceConsumer.produce(new RunTimeConfigurationSourceBuildItem(
                ApplicationPropertiesConfigSource.InFileSystem.class.getName(), OptionalInt.empty()));
    }

    /**
     * Write the default run time configuration.
     */
    @BuildStep
    RunTimeConfigurationSourceBuildItem writeDefaults(
            List<RunTimeConfigurationDefaultBuildItem> defaults,
            Consumer<GeneratedResourceBuildItem> resourceConsumer) throws IOException {
        final Properties properties = new Properties();
        for (RunTimeConfigurationDefaultBuildItem item : defaults) {
            final String key = item.getKey();
            final String value = item.getValue();
            final String existing = properties.getProperty(key);
            if (existing != null && !existing.equals(value)) {
                log.warnf(
                        "Two conflicting default values were specified for configuration key \"%s\": \"%s\" and \"%s\" (using \"%2$s\")",
                        key,
                        existing,
                        value);
            } else {
                properties.setProperty(key, value);
            }
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                properties.store(osw, "This is the generated set of default configuration values");
                osw.flush();
                resourceConsumer.accept(
                        new GeneratedResourceBuildItem(DefaultConfigSource.DEFAULT_CONFIG_PROPERTIES_NAME, os.toByteArray()));
            }
        }
        return new RunTimeConfigurationSourceBuildItem(DefaultConfigSource.class.getName(), OptionalInt.empty());
    }

    /**
     * Generate the bytecode to load configuration objects at static init and run time.
     *
     * @param runTimeConfigItem the config build item
     * @param classConsumer the consumer of generated classes
     * @param runTimeInitConsumer the consumer of runtime init classes
     */
    @BuildStep
    void finalizeConfigLoader(
            RunTimeConfigurationBuildItem runTimeConfigItem,
            BuildTimeRunTimeFixedConfigurationBuildItem buildTimeRunTimeConfigItem,
            Consumer<GeneratedClassBuildItem> classConsumer,
            Consumer<RuntimeInitializedClassBuildItem> runTimeInitConsumer,
            Consumer<BytecodeRecorderObjectLoaderBuildItem> objectLoaderConsumer,
            List<ConfigurationCustomConverterBuildItem> converters,
            List<RunTimeConfigurationSourceBuildItem> runTimeSources) {
        final ClassOutput classOutput = new ClassOutput() {
            public void write(final String name, final byte[] data) {
                classConsumer.accept(new GeneratedClassBuildItem(false, name, data));
            }
        };

        // General run time setup

        AccessorFinder accessorFinder = new AccessorFinder();

        final ConfigDefinition runTimeConfigDef = runTimeConfigItem.getConfigDefinition();
        final ConfigPatternMap<LeafConfigType> runTimePatterns = runTimeConfigDef.getLeafPatterns();

        runTimeConfigDef.generateConfigRootClass(classOutput, accessorFinder);

        final ConfigDefinition buildTimeConfigDef = buildTimeRunTimeConfigItem.getConfigDefinition();
        final ConfigPatternMap<LeafConfigType> buildTimePatterns = buildTimeConfigDef.getLeafPatterns();

        buildTimeConfigDef.generateConfigRootClass(classOutput, accessorFinder);

        // Traverse all known run-time config types and ensure we have converters for them when image building runs
        // This code is specific to native image and run time config, because the build time config is read during static init

        final HashSet<Class<?>> encountered = new HashSet<>();
        final ArrayList<Class<?>> configTypes = new ArrayList<>();
        for (LeafConfigType item : runTimePatterns) {
            final Class<?> typeClass = item.getItemClass();
            if (!typeClass.isPrimitive() && encountered.add(typeClass)
                    && ConverterFactory.getImplicitConverter(typeClass) != null) {
                configTypes.add(typeClass);
            }
        }
        // stability
        configTypes.sort(Comparator.comparing(Class::getName));
        int converterCnt = configTypes.size();

        // Build time configuration class, also holds converters
        try (final ClassCreator cc = new ClassCreator(classOutput, BUILD_TIME_CONFIG, null, Object.class.getName())) {
            // field to stash converters into
            cc.getFieldCreator(CONVERTERS_FIELD).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
            // holder for the build-time configuration
            cc.getFieldCreator(BUILD_TIME_CONFIG_FIELD)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE);

            // static init block
            try (MethodCreator clinit = cc.getMethodCreator("<clinit>", void.class)) {
                clinit.setModifiers(Opcodes.ACC_STATIC);

                // make implicit converters available to native image run time

                final BranchResult inImageBuild = clinit.ifNonZero(clinit
                        .invokeStaticMethod(MethodDescriptor.ofMethod(ImageInfo.class, "inImageBuildtimeCode", boolean.class)));
                try (BytecodeCreator yes = inImageBuild.trueBranch()) {
                    final ResultHandle array = yes.newArray(Converter.class, yes.load(converterCnt));
                    for (int i = 0; i < converterCnt; i++) {
                        yes.writeArrayValue(array, i,
                                yes.invokeStaticMethod(CF_GET_IMPLICIT_CONVERTER, yes.loadClass(configTypes.get(i))));
                    }
                    yes.writeStaticField(CONVERTERS_FIELD, array);
                }
                try (BytecodeCreator no = inImageBuild.falseBranch()) {
                    no.writeStaticField(CONVERTERS_FIELD, no.loadNull());
                }

                // create build time configuration object

                final ResultHandle builder = clinit.newInstance(SRCB_CONSTRUCT);
                // todo: custom build time converters
                final ResultHandle array = clinit.newArray(ConfigSource[].class, clinit.load(1));
                clinit.writeArrayValue(array, 0, clinit.invokeStaticMethod(BTCF_GET_CONFIG_SOURCE));

                // create the actual config object
                final ResultHandle config = clinit.checkCast(clinit.invokeVirtualMethod(SRCB_BUILD, builder),
                        SmallRyeConfig.class);

                // create the config root
                clinit.writeStaticField(BUILD_TIME_CONFIG_FIELD, clinit
                        .newInstance(MethodDescriptor.ofConstructor(BUILD_TIME_CONFIG_ROOT, SmallRyeConfig.class), config));

                // write out the parsing for the stored build time config
                writeParsing(cc, clinit, config, buildTimePatterns);

                clinit.returnValue(null);
            }
        }

        // Run time configuration class
        try (final ClassCreator cc = new ClassCreator(classOutput, RUN_TIME_CONFIG, null, Object.class.getName())) {
            // holder for the run-time configuration
            cc.getFieldCreator(RUN_TIME_CONFIG_FIELD)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE);

            // config object initialization
            try (MethodCreator carc = cc.getMethodCreator(ConfigurationSetup.CREATE_RUN_TIME_CONFIG)) {
                carc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

                // create run time configuration object
                final ResultHandle builder = carc.newInstance(SRCB_CONSTRUCT);
                carc.invokeVirtualMethod(SRCB_ADD_DEFAULT_SOURCES, builder);

                // custom run time sources
                final int size = runTimeSources.size();
                if (size > 0) {
                    final ResultHandle arrayHandle = carc.newArray(ConfigSource[].class, carc.load(size));
                    for (int i = 0; i < size; i++) {
                        final RunTimeConfigurationSourceBuildItem source = runTimeSources.get(i);
                        final OptionalInt priority = source.getPriority();
                        final ResultHandle val;
                        if (priority.isPresent()) {
                            val = carc.newInstance(MethodDescriptor.ofConstructor(source.getClassName(), int.class),
                                    carc.load(priority.getAsInt()));
                        } else {
                            val = carc.newInstance(MethodDescriptor.ofConstructor(source.getClassName()));
                        }
                        carc.writeArrayValue(arrayHandle, i, val);
                    }
                    carc.invokeVirtualMethod(
                            SRCB_WITH_SOURCES,
                            builder,
                            arrayHandle);
                }

                // custom run time converters
                for (ConfigurationCustomConverterBuildItem converter : converters) {
                    carc.invokeVirtualMethod(
                            SRCB_WITH_CONVERTER,
                            builder,
                            carc.loadClass(converter.getType()),
                            carc.load(converter.getPriority()),
                            carc.newInstance(MethodDescriptor.ofConstructor(converter.getConverter())));
                }

                // property expansion
                final ResultHandle wrapper = carc.readStaticField(ECS_WRAPPER);
                carc.invokeVirtualMethod(SRCB_WITH_WRAPPER, builder, wrapper);

                // write out loader for converter types
                final BranchResult imgRun = carc.ifNonZero(carc.invokeStaticMethod(II_IN_IMAGE_RUN));
                try (BytecodeCreator inImageRun = imgRun.trueBranch()) {
                    final ResultHandle array = inImageRun.readStaticField(CONVERTERS_FIELD);
                    for (int i = 0; i < converterCnt; i++) {
                        // implicit converters will have a priority of 100.
                        inImageRun.invokeVirtualMethod(
                                SRCB_WITH_CONVERTER,
                                builder,
                                inImageRun.loadClass(configTypes.get(i)),
                                inImageRun.load(100),
                                inImageRun.readArrayValue(array, i));
                    }
                }

                // Build the config

                final ResultHandle config = carc.checkCast(carc.invokeVirtualMethod(SRCB_BUILD, builder), SmallRyeConfig.class);
                final ResultHandle providerResolver = carc.newInstance(SCPR_CONSTRUCT, config);
                carc.invokeStaticMethod(CPR_SET_INSTANCE, providerResolver);

                // create the config root
                carc.writeStaticField(RUN_TIME_CONFIG_FIELD,
                        carc.newInstance(MethodDescriptor.ofConstructor(RUN_TIME_CONFIG_ROOT, SmallRyeConfig.class), config));

                writeParsing(cc, carc, config, runTimePatterns);

                carc.returnValue(null);
            }
        }

        objectLoaderConsumer.accept(new BytecodeRecorderObjectLoaderBuildItem(new ObjectLoader() {
            public ResultHandle load(final BytecodeCreator body, final Object obj, final boolean staticInit) {
                boolean buildTime = false;
                ConfigDefinition.RootInfo rootInfo = runTimeConfigDef.getInstanceInfo(obj);
                if (rootInfo == null) {
                    rootInfo = buildTimeConfigDef.getInstanceInfo(obj);
                    buildTime = true;
                }
                if (rootInfo == null || staticInit && !buildTime) {
                    final Class<?> objClass = obj.getClass();
                    if (objClass.isAnnotationPresent(ConfigRoot.class)) {
                        String msg = String.format(
                                "You are trying to use a ConfigRoot[%s] at static initialization time",
                                objClass.getName());
                        throw new IllegalStateException(msg);
                    }
                    return null;
                }

                final FieldDescriptor fieldDescriptor = rootInfo.getFieldDescriptor();
                final ResultHandle configRoot = body
                        .readStaticField(buildTime ? BUILD_TIME_CONFIG_FIELD : RUN_TIME_CONFIG_FIELD);
                return body.readInstanceField(fieldDescriptor, configRoot);
            }
        }));

        runTimeInitConsumer.accept(new RuntimeInitializedClassBuildItem(RUN_TIME_CONFIG));
    }

    private void writeParsing(final ClassCreator cc, final BytecodeCreator body, final ResultHandle config,
            final ConfigPatternMap<LeafConfigType> keyMap) {
        // setup
        // Iterable iterable = config.getPropertyNames();
        final ResultHandle iterable = body.invokeVirtualMethod(
                MethodDescriptor.ofMethod(SmallRyeConfig.class, "getPropertyNames", Iterable.class), config);
        // Iterator iterator = iterable.iterator();
        final ResultHandle iterator = body
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), iterable);

        // loop: {
        try (BytecodeCreator loop = body.createScope()) {
            // if (iterator.hasNext())
            final BranchResult ifHasNext = loop.ifNonZero(loop.invokeInterfaceMethod(ITR_HAS_NEXT, iterator));
            // {
            try (BytecodeCreator hasNext = ifHasNext.trueBranch()) {
                // key = iterator.next();
                final ResultHandle key = hasNext.checkCast(hasNext.invokeInterfaceMethod(ITR_NEXT, iterator), String.class);
                // NameIterator keyIter = new NameIterator(key);
                final ResultHandle keyIter = hasNext
                        .newInstance(MethodDescriptor.ofConstructor(NameIterator.class, String.class), key);
                // if (! keyIter.hasNext()) continue loop;
                hasNext.ifNonZero(hasNext.invokeVirtualMethod(NI_HAS_NEXT, keyIter)).falseBranch().continueScope(loop);
                // if (! keyIter.nextSegmentEquals("quarkus")) continue loop;
                hasNext.ifNonZero(hasNext.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, hasNext.load("quarkus"))).falseBranch()
                        .continueScope(loop);
                // keyIter.next(); // skip "quarkus"
                hasNext.invokeVirtualMethod(NI_NEXT, keyIter);
                // parse(config, keyIter);
                hasNext.invokeStaticMethod(generateParserBody(cc, keyMap, new StringBuilder("parseKey"), new HashMap<>()),
                        config, keyIter);
                // continue loop;
                hasNext.continueScope(loop);
            }
            // }
        }
        // }
        body.returnValue(body.loadNull());
    }

    private MethodDescriptor generateParserBody(final ClassCreator cc, final ConfigPatternMap<LeafConfigType> keyMap,
            final StringBuilder methodName, final Map<String, MethodDescriptor> parseMethodCache) {
        final String methodNameStr = methodName.toString();
        final MethodDescriptor existing = parseMethodCache.get(methodNameStr);
        if (existing != null)
            return existing;
        try (MethodCreator body = cc.getMethodCreator(methodName.toString(), void.class, SmallRyeConfig.class,
                NameIterator.class)) {
            body.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            final ResultHandle config = body.getMethodParam(0);
            final ResultHandle keyIter = body.getMethodParam(1);
            final LeafConfigType matched = keyMap.getMatched();
            // if (! keyIter.hasNext()) {
            try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter)).falseBranch()) {
                if (matched != null) {
                    // (exact match generated code)
                    matched.generateAcceptConfigurationValue(matchedBody, keyIter, config);
                } else {
                    // todo: unknown name warning goes here
                }
                // return;
                matchedBody.returnValue(null);
            }
            // }
            // branches for each next-string
            boolean hasWildCard = false;
            final Iterable<String> names = keyMap.childNames();
            for (String name : names) {
                if (name.equals(ConfigPatternMap.WILD_CARD)) {
                    hasWildCard = true;
                } else {
                    // TODO: string switch
                    // if (keyIter.nextSegmentEquals(name)) {
                    try (BytecodeCreator nameMatched = body
                            .ifNonZero(body.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, body.load(name))).trueBranch()) {
                        // keyIter.next();
                        nameMatched.invokeVirtualMethod(NI_NEXT, keyIter);
                        // (generated recursive)
                        final int length = methodName.length();
                        methodName.append('_').append(name);
                        nameMatched.invokeStaticMethod(
                                generateParserBody(cc, keyMap.getChild(name), methodName, parseMethodCache), config, keyIter);
                        methodName.setLength(length);
                        // return;
                        nameMatched.returnValue(null);
                    }
                    // }
                }
            }
            if (hasWildCard) {
                // consume and parse
                try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter))
                        .trueBranch()) {
                    // keyIter.next();
                    matchedBody.invokeVirtualMethod(NI_NEXT, keyIter);
                    // (generated recursive)
                    final int length = methodName.length();
                    methodName.append('_').append("wildcard");
                    matchedBody.invokeStaticMethod(
                            generateParserBody(cc, keyMap.getChild(ConfigPatternMap.WILD_CARD), methodName, parseMethodCache),
                            config, keyIter);
                    methodName.setLength(length);
                    // return;
                    matchedBody.returnValue(null);
                }
            }
            // todo: unknown name warning goes here
            body.returnValue(null);
            final MethodDescriptor md = body.getMethodDescriptor();
            parseMethodCache.put(methodNameStr, md);
            return md;
        }
    }

    @BuildStep
    void writeDefaultConfiguration(

    ) {

    }
}
