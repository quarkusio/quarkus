package io.quarkus.deployment.steps;

import static io.quarkus.deployment.util.ReflectUtil.toError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.objectweb.asm.Opcodes;
import org.wildfly.common.net.CidrAddress;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.ConfigurationCustomConverterBuildItem;
import io.quarkus.deployment.builditem.ExtensionClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.configuration.ConfigDefinition;
import io.quarkus.deployment.configuration.ConfigPatternMap;
import io.quarkus.deployment.configuration.LeafConfigType;
import io.quarkus.deployment.recording.ObjectLoader;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.configuration.CidrAddressConverter;
import io.quarkus.runtime.configuration.ConverterFactory;
import io.quarkus.runtime.configuration.DefaultConfigSource;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.InetAddressConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.runtime.configuration.RegexConverter;
import io.quarkus.runtime.configuration.SimpleConfigurationProviderResolver;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Setup steps for configuration purposes.
 */
public class ConfigurationSetup {

    private static final Logger log = Logger.getLogger("io.quarkus.configuration");

    public static final String CONFIG_HELPER = "io.quarkus.runtime.generated.ConfigHelper";
    public static final String CONFIG_HELPER_DATA = "io.quarkus.runtime.generated.ConfigHelperData";
    public static final String CONFIG_ROOT = "io.quarkus.runtime.generated.ConfigRoot";

    public static final FieldDescriptor CONFIG_ROOT_FIELD = FieldDescriptor.of(CONFIG_HELPER_DATA, "configRoot", CONFIG_ROOT);

    private static final MethodDescriptor NI_HAS_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor NI_NEXT_EQUALS = MethodDescriptor.ofMethod(NameIterator.class, "nextSegmentEquals",
            boolean.class, String.class);
    private static final MethodDescriptor NI_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);
    private static final MethodDescriptor ITR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor ITR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);
    private static final MethodDescriptor CF_GET_CONVERTER = MethodDescriptor.ofMethod(ConverterFactory.class, "getConverter",
            Converter.class, SmallRyeConfig.class, Class.class);
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
    private static final MethodDescriptor II_IN_IMAGE_BUILD = MethodDescriptor.ofMethod(ImageInfo.class, "inImageBuildtimeCode",
            boolean.class);
    private static final MethodDescriptor II_IN_IMAGE_RUN = MethodDescriptor.ofMethod(ImageInfo.class, "inImageRuntimeCode",
            boolean.class);
    private static final MethodDescriptor SRCB_WITH_WRAPPER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withWrapper", SmallRyeConfigBuilder.class, UnaryOperator.class);

    public static final MethodDescriptor GET_ROOT_METHOD = MethodDescriptor.ofMethod(CONFIG_HELPER, "getRoot", CONFIG_ROOT);

    private static final FieldDescriptor ECS_WRAPPER = FieldDescriptor.of(ExpandingConfigSource.class, "WRAPPER",
            UnaryOperator.class);

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
    }

    /**
     * Run before anything that consumes configuration; sets up the main configuration definition instance.
     *
     * @param converters the converters to set up
     * @return the configuration build item
     */
    @BuildStep
    public ConfigurationBuildItem initializeConfiguration(
            List<ConfigurationCustomConverterBuildItem> converters,
            ExtensionClassLoaderBuildItem extensionClassLoaderBuildItem) throws IOException, ClassNotFoundException {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        // expand properties
        builder.withWrapper(ExpandingConfigSource::new);
        builder.addDefaultSources();
        for (ConfigurationCustomConverterBuildItem converter : converters) {
            withConverterHelper(builder, converter.getType(), converter.getPriority(), converter.getConverter());
        }
        final SmallRyeConfig src = (SmallRyeConfig) builder.build();
        final ConfigDefinition configDefinition = new ConfigDefinition();
        // populate it with all known types
        for (Class<?> clazz : ServiceUtil.classesNamedIn(extensionClassLoaderBuildItem.getExtensionClassLoader(),
                "META-INF/quarkus-config-roots.list")) {
            configDefinition.registerConfigRoot(clazz);
        }
        SmallRyeConfigProviderResolver.instance().registerConfig(src, Thread.currentThread().getContextClassLoader());
        configDefinition.loadConfiguration(src);
        return new ConfigurationBuildItem(configDefinition);
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
     * @param configurationBuildItem the config build item
     * @param classConsumer the consumer of generated classes
     * @param runTimeInitConsumer the consumer of runtime init classes
     */
    @BuildStep
    void finalizeConfigLoader(
            ConfigurationBuildItem configurationBuildItem,
            Consumer<GeneratedClassBuildItem> classConsumer,
            Consumer<RuntimeReinitializedClassBuildItem> runTimeInitConsumer,
            Consumer<BytecodeRecorderObjectLoaderBuildItem> objectLoaderConsumer,
            List<ConfigurationCustomConverterBuildItem> converters,
            List<RunTimeConfigurationSourceBuildItem> runTimeSources) {
        final ClassOutput classOutput = new ClassOutput() {
            public void write(final String name, final byte[] data) {
                classConsumer.accept(new GeneratedClassBuildItem(false, name, data));
            }
        };
        // Get the set of run time and static init leaf keys
        final ConfigDefinition configDefinition = configurationBuildItem.getConfigDefinition();

        final ConfigPatternMap<LeafConfigType> allLeafPatterns = configDefinition.getLeafPatterns();
        final ConfigPatternMap<LeafConfigType> runTimePatterns = new ConfigPatternMap<>();
        final ConfigPatternMap<LeafConfigType> staticInitPatterns = new ConfigPatternMap<>();
        for (String childName : allLeafPatterns.childNames()) {
            ConfigPhase phase = configDefinition.getPhaseByKey(childName);
            if (phase.isReadAtMain()) {
                runTimePatterns.addChild(childName, allLeafPatterns.getChild(childName));
            }
            if (phase.isReadAtStaticInit()) {
                staticInitPatterns.addChild(childName, allLeafPatterns.getChild(childName));
            }
        }

        AccessorFinder accessorMaker = new AccessorFinder();

        configDefinition.generateConfigRootClass(classOutput, accessorMaker);

        final FieldDescriptor convertersField;
        // This must be a separate class, because CONFIG_HELPER is re-initialized at run time (native image).
        try (final ClassCreator cc = new ClassCreator(classOutput, CONFIG_HELPER_DATA, null, Object.class.getName())) {
            convertersField = cc.getFieldCreator("$CONVERTERS", Converter[].class)
                    .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE).getFieldDescriptor();
            cc.getFieldCreator(CONFIG_ROOT_FIELD).setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE)
                    .getFieldDescriptor();
        }

        try (final ClassCreator cc = new ClassCreator(classOutput, CONFIG_HELPER, null, Object.class.getName())) {
            final MethodDescriptor createAndRegisterConfig;
            // config object initialization
            // this has to be on the static init class, which is visible at both static init and execution time
            try (MethodCreator carc = cc.getMethodCreator("createAndRegisterConfig", SmallRyeConfig.class)) {
                carc.setModifiers(Opcodes.ACC_STATIC);
                final ResultHandle builder = carc.newInstance(SRCB_CONSTRUCT);
                carc.invokeVirtualMethod(SRCB_ADD_DEFAULT_SOURCES, builder);
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
                for (ConfigurationCustomConverterBuildItem converter : converters) {
                    carc.invokeVirtualMethod(
                            SRCB_WITH_CONVERTER,
                            builder,
                            carc.loadClass(converter.getType()),
                            carc.load(converter.getPriority()),
                            carc.newInstance(MethodDescriptor.ofConstructor(converter.getConverter())));
                }
                // todo: add custom sources
                final ResultHandle wrapper = carc.readStaticField(ECS_WRAPPER);
                carc.invokeVirtualMethod(SRCB_WITH_WRAPPER, builder, wrapper);

                // Traverse all known config types and ensure we have converters for them when image building runs
                // This code is specific to native image

                HashSet<Class<?>> encountered = new HashSet<>();
                ArrayList<Class<?>> configTypes = new ArrayList<>();
                for (LeafConfigType item : allLeafPatterns) {
                    final Class<?> typeClass = item.getItemClass();
                    if (!typeClass.isPrimitive() && encountered.add(typeClass)) {
                        configTypes.add(typeClass);
                    }
                }
                // stability
                configTypes.sort(Comparator.comparing(Class::getName));
                int cnt = configTypes.size();

                // At image runtime, load the converters array and register it with the config builder
                // This code is specific to native image

                final BranchResult imgRun = carc.ifNonZero(carc.invokeStaticMethod(II_IN_IMAGE_RUN));
                try (BytecodeCreator inImageRun = imgRun.trueBranch()) {
                    final ResultHandle array = inImageRun.readStaticField(convertersField);
                    for (int i = 0; i < cnt; i++) {
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

                // At image build time, record all the implicit converts and store them in the converters array
                // This actually happens before the above `if` block, despite necessarily coming later in the method sequence
                // This code is specific to native image

                final BranchResult imgBuild = carc.ifNonZero(carc.invokeStaticMethod(II_IN_IMAGE_BUILD));
                try (BytecodeCreator inImageBuild = imgBuild.trueBranch()) {
                    final ResultHandle array = inImageBuild.newArray(Converter.class, inImageBuild.load(cnt));
                    for (int i = 0; i < cnt; i++) {
                        inImageBuild.writeArrayValue(array, i, inImageBuild.invokeStaticMethod(CF_GET_CONVERTER, config,
                                inImageBuild.loadClass(configTypes.get(i))));
                    }
                    inImageBuild.writeStaticField(convertersField, array);
                }

                carc.returnValue(carc.checkCast(config, SmallRyeConfig.class));
                createAndRegisterConfig = carc.getMethodDescriptor();
            }

            // helper to ensure the config is instantiated before it is read
            try (MethodCreator getRoot = cc.getMethodCreator("getRoot", CONFIG_ROOT)) {
                getRoot.setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC);

                getRoot.returnValue(getRoot.readStaticField(CONFIG_ROOT_FIELD));
            }

            // static init block
            try (MethodCreator ccInit = cc.getMethodCreator("<clinit>", void.class)) {
                ccInit.setModifiers(Opcodes.ACC_STATIC);

                // write out the parsing
                final BranchResult ccIfImage = ccInit.ifNonZero(ccInit
                        .invokeStaticMethod(MethodDescriptor.ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class)));
                try (BytecodeCreator ccIsNotImage = ccIfImage.falseBranch()) {
                    // common case: JVM mode, or image-building initialization
                    final ResultHandle mccConfig = ccIsNotImage.invokeStaticMethod(createAndRegisterConfig);
                    ccIsNotImage.newInstance(MethodDescriptor.ofConstructor(CONFIG_ROOT, SmallRyeConfig.class), mccConfig);
                    writeParsing(cc, ccIsNotImage, mccConfig, staticInitPatterns);
                }
                try (BytecodeCreator ccIsImage = ccIfImage.trueBranch()) {
                    // native image run time only (class reinitialization)
                    final ResultHandle mccConfig = ccIsImage.invokeStaticMethod(createAndRegisterConfig);
                    writeParsing(cc, ccIsImage, mccConfig, runTimePatterns);
                }
                ccInit.returnValue(null);
            }
        }

        objectLoaderConsumer.accept(new BytecodeRecorderObjectLoaderBuildItem(new ObjectLoader() {
            public ResultHandle load(final BytecodeCreator body, final Object obj) {
                final ConfigDefinition.RootInfo rootInfo = configDefinition.getInstanceInfo(obj);
                if (rootInfo == null)
                    return null;

                if (!rootInfo.getConfigPhase().isAvailableAtRun()) {
                    String msg = String.format(
                            "You are trying to use a ConfigRoot[%s] at runtime whose phase[%s] does not allow this",
                            rootInfo.getRootClass().getName(), rootInfo.getConfigPhase());
                    throw new IllegalStateException(msg);
                }
                final FieldDescriptor fieldDescriptor = rootInfo.getFieldDescriptor();
                final ResultHandle configRoot = body.invokeStaticMethod(GET_ROOT_METHOD);
                return body.readInstanceField(fieldDescriptor, configRoot);
            }
        }));

        runTimeInitConsumer.accept(new RuntimeReinitializedClassBuildItem(CONFIG_HELPER));
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
            final Iterable<String> names = keyMap.childNames();
            for (String name : names) {
                if (name.equals(ConfigPatternMap.WILD_CARD)) {
                    // skip
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
