package org.jboss.shamrock.deployment.steps;

import static org.jboss.shamrock.deployment.util.ReflectUtil.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.shamrock.deployment.AccessorFinder;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationCustomConverterBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationRegistrationBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationRunTimeKeyBuildItem;
import org.jboss.shamrock.deployment.builditem.ConfigurationTypeBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import org.jboss.shamrock.deployment.configuration.BooleanConfigType;
import org.jboss.shamrock.deployment.configuration.CompoundConfigType;
import org.jboss.shamrock.deployment.configuration.ConfigDefinition;
import org.jboss.shamrock.deployment.configuration.ConfigPatternMap;
import org.jboss.shamrock.deployment.configuration.GroupConfigType;
import org.jboss.shamrock.deployment.configuration.IntConfigType;
import org.jboss.shamrock.deployment.configuration.LeafConfigType;
import org.jboss.shamrock.deployment.configuration.MapConfigType;
import org.jboss.shamrock.deployment.configuration.ObjectConfigType;
import org.jboss.shamrock.deployment.configuration.ObjectListConfigType;
import org.jboss.shamrock.deployment.configuration.OptionalObjectConfigType;
import org.jboss.shamrock.deployment.recording.ObjectLoader;
import org.jboss.shamrock.deployment.util.StringUtil;
import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.configuration.ConverterFactory;
import org.jboss.shamrock.runtime.configuration.ExpandingConfigSource;
import org.jboss.shamrock.runtime.configuration.NameIterator;
import org.jboss.shamrock.runtime.configuration.SimpleConfigurationProviderResolver;
import org.objectweb.asm.Opcodes;

/**
 * Setup steps for configuration purposes.
 */
public class ConfigurationSetup {

    public static final String NO_CONTAINING_NAME = "<<ignored>>";
    public static final String CONFIG_HELPER = "org.jboss.shamrock.runtime.generated.ConfigHelper";
    private static final String CONFIG_HELPER_DATA = "org.jboss.shamrock.runtime.generated.ConfigHelperData";

    private static final MethodDescriptor NI_HAS_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor NI_NEXT_EQUALS = MethodDescriptor.ofMethod(NameIterator.class, "nextSegmentEquals", boolean.class, String.class);
    private static final MethodDescriptor NI_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);
    private static final MethodDescriptor ITR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);
    private static final MethodDescriptor ITR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);
    private static final MethodDescriptor CF_GET_CONVERTER = MethodDescriptor.ofMethod(ConverterFactory.class, "getConverter", Converter.class, SmallRyeConfig.class, Class.class);
    private static final MethodDescriptor CPR_SET_INSTANCE = MethodDescriptor.ofMethod(ConfigProviderResolver.class, "setInstance", void.class, ConfigProviderResolver.class);
    private static final MethodDescriptor SCPR_CONSTRUCT = MethodDescriptor.ofConstructor(SimpleConfigurationProviderResolver.class, Config.class);
    private static final MethodDescriptor SRCB_BUILD = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "build", Config.class);
    private static final MethodDescriptor SRCB_WITH_CONVERTER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "withConverter", ConfigBuilder.class, Class.class, int.class, Converter.class);
    private static final MethodDescriptor SRCB_ADD_DEFAULT_SOURCES = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "addDefaultSources", ConfigBuilder.class);
    private static final MethodDescriptor SRCB_CONSTRUCT = MethodDescriptor.ofConstructor(SmallRyeConfigBuilder.class);
    private static final MethodDescriptor II_IN_IMAGE_BUILD = MethodDescriptor.ofMethod(ImageInfo.class, "inImageBuildtimeCode", boolean.class);
    private static final MethodDescriptor II_IN_IMAGE_RUN = MethodDescriptor.ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class);
    private static final MethodDescriptor SRCB_WITH_WRAPPER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "withWrapper", SmallRyeConfigBuilder.class, UnaryOperator.class);

    private static final FieldDescriptor ECS_WRAPPER = FieldDescriptor.of(ExpandingConfigSource.class, "WRAPPER", UnaryOperator.class);

    public ConfigurationSetup() {}

    /**
     * Run before anything that consumes configuration; sets up the main configuration definition instance.
     *
     * @param rootItems the registered root items
     * @return the configuration build item
     */
    @BuildStep
    public ConfigurationBuildItem initializeConfiguration(
        List<ConfigurationRegistrationBuildItem> rootItems,
        List<ConfigurationCustomConverterBuildItem> converters
    ) {
        final AccessorFinder accessorFinder = new AccessorFinder();
        final ConfigDefinition configDefinition = new ConfigDefinition();
        for (ConfigurationRegistrationBuildItem rootItem : rootItems) {
            final String baseKey = rootItem.getBaseKey();
            final Type type = rootItem.getType();
            // parse out the type
            final Class<?> rawType = rawTypeOf(type);
            final AnnotatedElement site = rootItem.getInjectionSite();
            if (rawType == Map.class && rawTypeOfParameter(type, 0) == String.class) {
                // check key type
                processMap(baseKey, configDefinition, site, true, baseKey, typeOfParameter(type, 1), accessorFinder);
            } else if (rawType.isAnnotationPresent(ConfigGroup.class)) {
                processConfigGroup(baseKey, configDefinition, true, baseKey, rawType, accessorFinder);
            }
        }
        configDefinition.load();

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        // expand properties
        builder.withWrapper(ExpandingConfigSource::new);
        builder.addDefaultSources();
        for (ConfigurationCustomConverterBuildItem converter : converters) {
            withConverterHelper(builder, converter.getType(), converter.getPriority(), converter.getConverter());
        }
        final SmallRyeConfig src = (SmallRyeConfig) builder.build();
        configDefinition.loadConfiguration(src);
        return new ConfigurationBuildItem(configDefinition);
    }

    @SuppressWarnings("unchecked")
    private static <T> void withConverterHelper(final SmallRyeConfigBuilder builder, final Class<T> type, final int priority, final Class<? extends Converter<?>> converterClass) {
        try {
            builder.withConverter(type, priority, ((Class<? extends Converter<T>>) converterClass).newInstance());
        } catch (InstantiationException e) {
            throw toError(e);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    private GroupConfigType processConfigGroup(final String containingName, final CompoundConfigType container, final boolean consumeSegment, final String baseKey, final Class<?> configGroupClass, final AccessorFinder accessorFinder) {
        GroupConfigType gct = new GroupConfigType(containingName, container, consumeSegment, configGroupClass, accessorFinder);
        if (gct.isRoot()) gct.registerRootType(gct, accessorFinder);
        final Field[] fields = configGroupClass.getDeclaredFields();
        for (Field field : fields) {
            final ConfigItem configItemAnnotation = field.getAnnotation(ConfigItem.class);
            final String name = configItemAnnotation == null ? StringUtil.hyphenate(field.getName()) : configItemAnnotation.name();
            String subKey;
            boolean consume;
            if (name.equals(ConfigItem.PARENT)) {
                subKey = baseKey;
                consume = false;
            } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
                subKey = baseKey + "." + field.getName();
                consume = true;
            } else if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                subKey = baseKey + "." + StringUtil.hyphenate(field.getName());
                consume = true;
            } else {
                subKey = baseKey + "." + name;
                consume = true;
            }
            final String defaultValue = configItemAnnotation == null ? ConfigItem.NO_DEFAULT : configItemAnnotation.defaultValue();
            final Type fieldType = field.getGenericType();
            final Class<?> fieldClass = field.getType();
            if (fieldClass.isAnnotationPresent(ConfigGroup.class)) {
                if (! defaultValue.equals(ConfigItem.NO_DEFAULT)) {
                    throw reportError(field, "Unsupported default value");
                }
                gct.addField(processConfigGroup(field.getName(), gct, consume, subKey, fieldClass, accessorFinder));
            } else if (fieldClass.isPrimitive()) {
                final LeafConfigType leaf;
                if (fieldClass == boolean.class) {
                    gct.addField(leaf = new BooleanConfigType(field.getName(), gct, consume, defaultValue.equals(ConfigItem.NO_DEFAULT) ? "false" : defaultValue));
                } else if (fieldClass == int.class) {
                    gct.addField(leaf = new IntConfigType(field.getName(), gct, consume, defaultValue.equals(ConfigItem.NO_DEFAULT) ? "0" : defaultValue));
                } else {
                    throw reportError(field, "Unsupported primitive field type");
                }
                container.getConfigDefinition().getLeafPatterns().addPattern(subKey, leaf);
            } else if (fieldClass == Map.class) {
                if (rawTypeOfParameter(fieldType, 0) != String.class) {
                    throw reportError(field, "Map key must be " + String.class);
                }
                gct.addField(processMap(field.getName(), gct, field, consume, subKey, typeOfParameter(fieldType, 1), accessorFinder));
            } else if (fieldClass == List.class) {
                // list leaf class
                final LeafConfigType leaf;
                final Class<?> listType = rawTypeOfParameter(fieldType, 0);
                gct.addField(leaf = new ObjectListConfigType(field.getName(), gct, consume, defaultValue.equals(ConfigItem.NO_DEFAULT) ? "" : defaultValue, listType));
                container.getConfigDefinition().getLeafPatterns().addPattern(subKey, leaf);
            } else if (fieldClass == Optional.class) {
                final LeafConfigType leaf;
                // optional config property
                gct.addField(leaf = new OptionalObjectConfigType(field.getName(), gct, consume, defaultValue.equals(ConfigItem.NO_DEFAULT) ? "" : defaultValue, rawTypeOfParameter(fieldType, 0)));
                container.getConfigDefinition().getLeafPatterns().addPattern(subKey, leaf);
            } else {
                final LeafConfigType leaf;
                // it's a plain config property
                gct.addField(leaf = new ObjectConfigType(field.getName(), gct, consume, defaultValue.equals(ConfigItem.NO_DEFAULT) ? "" : defaultValue, fieldClass));
                container.getConfigDefinition().getLeafPatterns().addPattern(subKey, leaf);
            }
        }
        return gct;
    }

    private MapConfigType processMap(final String containingName, final CompoundConfigType container, final AnnotatedElement containingElement, final boolean consumeSegment, final String baseKey, final Type mapValueType, final AccessorFinder accessorFinder) {
        MapConfigType mct = new MapConfigType(containingName, container, consumeSegment);
        if (mct.isRoot()) mct.registerRootType(mct, accessorFinder);
        final Class<?> valueClass = rawTypeOf(mapValueType);
        final String subKey = baseKey + ".{**}";
        if (valueClass == Map.class) {
            if (! (mapValueType instanceof ParameterizedType)) throw reportError(containingElement, "Map must be parameterized");
            processMap(NO_CONTAINING_NAME, mct, containingElement, true, subKey, typeOfParameter(mapValueType, 1), accessorFinder);
        } else if (valueClass.isAnnotationPresent(ConfigGroup.class)) {
            processConfigGroup(NO_CONTAINING_NAME, mct, true, subKey, valueClass, accessorFinder);
        } else if (valueClass == List.class) {
            final ObjectListConfigType leaf = new ObjectListConfigType(NO_CONTAINING_NAME, mct, consumeSegment, "", rawTypeOfParameter(typeOfParameter(mapValueType, 1), 0));
            container.getConfigDefinition().getLeafPatterns().addPattern(subKey, leaf);
        } else if (valueClass == Optional.class || valueClass == OptionalInt.class || valueClass == OptionalDouble.class || valueClass == OptionalLong.class) {
            throw reportError(containingElement, "Optionals are not allowed as a map value type");
        } else {
            // treat as a plain object, hope for the best
            new ObjectConfigType(NO_CONTAINING_NAME, mct, true, "", valueClass);
        }
        return mct;
    }

    private static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
        if (e instanceof Member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Member) e).getDeclaringClass());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of " + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }

    /**
     * Generate the bytecode to load configuration objects at static init and run time.
     *
     * @param configurationBuildItem the config build item
     * @param runTimeKeys the list of configuration group/map keys to make available
     * @param classConsumer the consumer of generated classes
     * @param runTimeInitConsumer the consumer of runtime init classes
     */
    @BuildStep
    void finalizeConfigLoader(
        ConfigurationBuildItem configurationBuildItem,
        List<ConfigurationRunTimeKeyBuildItem> runTimeKeys,
        Consumer<GeneratedClassBuildItem> classConsumer,
        Consumer<RuntimeReinitializedClassBuildItem> runTimeInitConsumer,
        Consumer<BytecodeRecorderObjectLoaderBuildItem> objectLoaderConsumer,
        List<ConfigurationCustomConverterBuildItem> converters,
        List<ConfigurationTypeBuildItem> types
    ) {
        // hard-coded for now...
        Set<String> runTimeNames = new HashSet<>();
        Set<String> staticInitNames = new HashSet<>();
        for (ConfigurationRunTimeKeyBuildItem runTimeKey : runTimeKeys) {
            final EnumSet<ConfigPhase> phases = runTimeKey.getConfigPhases();
            if (phases.contains(ConfigPhase.MAIN)) {
                runTimeNames.add(runTimeKey.getBaseAddress());
            }
            if (phases.contains(ConfigPhase.STATIC_INIT)) {
                staticInitNames.add(runTimeKey.getBaseAddress());
            }
        }

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
            if (runTimeNames.contains(childName)) {
                runTimePatterns.addChild(childName, allLeafPatterns.getChild(childName));
            }
            if (staticInitNames.contains(childName)) {
                staticInitPatterns.addChild(childName, allLeafPatterns.getChild(childName));
            }
        }
        final Map<String, FieldDescriptor> fields = new HashMap<>();

        final FieldDescriptor convertersField;
        // This must be a separate class, because CONFIG_HELPER is re-initialized at run time (native image).
        // This class is never accessed in JVM mode.
        try (final ClassCreator cc = new ClassCreator(classOutput, CONFIG_HELPER_DATA, null, Object.class.getName())) {
            convertersField = cc.getFieldCreator("$CONVERTERS", Converter[].class).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE).getFieldDescriptor();
        }

        try (final ClassCreator cc = new ClassCreator(classOutput, CONFIG_HELPER, null, Object.class.getName())) {
            final MethodDescriptor createAndRegisterConfig;
            // config object initialization
            // this has to be on the static init class, which is visible at both static init and execution time
            try (MethodCreator carc = cc.getMethodCreator("createAndRegisterConfig", SmallRyeConfig.class)) {
                carc.setModifiers(Opcodes.ACC_STATIC);
                final ResultHandle builder = carc.newInstance(SRCB_CONSTRUCT);
                carc.invokeVirtualMethod(SRCB_ADD_DEFAULT_SOURCES, builder);
                for (ConfigurationCustomConverterBuildItem converter : converters) {
                    carc.invokeVirtualMethod(
                        SRCB_WITH_CONVERTER,
                        builder,
                        carc.loadClass(converter.getType()),
                        carc.load(converter.getPriority()),
                        carc.newInstance(MethodDescriptor.ofConstructor(converter.getConverter()))
                    );
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
                    if (! typeClass.isPrimitive() && encountered.add(typeClass)) {
                        configTypes.add(typeClass);
                    }
                }
                for (ConfigurationTypeBuildItem type : types) {
                    final Class<?> valueType = type.getValueType();
                    if (! valueType.isPrimitive() && encountered.add(valueType)) {
                        configTypes.add(valueType);
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
                    for (int i = 0; i < cnt; i ++) {
                        // implicit converters will have a priority of 100.
                        inImageRun.invokeVirtualMethod(
                            SRCB_WITH_CONVERTER,
                            builder,
                            inImageRun.loadClass(configTypes.get(i)),
                            inImageRun.load(100),
                            inImageRun.readArrayValue(array, i)
                        );
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
                    for (int i = 0; i < cnt; i ++) {
                        inImageBuild.writeArrayValue(array, i, inImageBuild.invokeStaticMethod(CF_GET_CONVERTER, config, inImageBuild.loadClass(configTypes.get(i))));
                    }
                    inImageBuild.writeStaticField(convertersField, array);
                }

                carc.returnValue(carc.checkCast(config, SmallRyeConfig.class));
                createAndRegisterConfig = carc.getMethodDescriptor();
            }

            // static init block
            AccessorFinder accessorMaker = new AccessorFinder();
            try (MethodCreator ccInit = cc.getMethodCreator("<clinit>", void.class)) {
                ccInit.setModifiers(Opcodes.ACC_STATIC);
                final ResultHandle ccConfig = ccInit.invokeStaticMethod(createAndRegisterConfig);
                for (ConfigurationRunTimeKeyBuildItem runTimeKey : runTimeKeys) {
                    final EnumSet<ConfigPhase> configPhases = runTimeKey.getConfigPhases();
                    if (! (configPhases.contains(ConfigPhase.STATIC_INIT) || configPhases.contains(ConfigPhase.MAIN))) {
                        // no field, no init
                        continue;
                    }
                    // first add a field for it
                    final String fieldName = runTimeKey.getExpectedType().getContainingName();
                    if (fields.containsKey(fieldName)) {
                        // don't duplicate the field
                        continue;
                    }
                    final FieldDescriptor fieldDescriptor = FieldDescriptor.of(CONFIG_HELPER, fieldName, Object.class);
                    cc.getFieldCreator(fieldDescriptor).setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
                    fields.put(fieldName, fieldDescriptor);

                    // initialize all fields to default values first
                    final MethodDescriptor initMethodDescr = MethodDescriptor.ofMethod(CONFIG_HELPER, "init:" + fieldName, Object.class, SmallRyeConfig.class);
                    ccInit.writeStaticField(
                        fieldDescriptor,
                        ccInit.invokeStaticMethod(initMethodDescr, ccConfig)
                    );

                    // write initialization method
                    try (MethodCreator initMethod = cc.getMethodCreator(initMethodDescr)) {
                        initMethod.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
                        initMethod.returnValue(runTimeKey.getExpectedType().writeInitialization(initMethod, accessorMaker, initMethod.getMethodParam(0)));
                    }
                }
                // now write out the parsing
                final BranchResult ccIfImage = ccInit.ifNonZero(ccInit.invokeStaticMethod(MethodDescriptor.ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class)));
                try (BytecodeCreator ccIsNotImage = ccIfImage.falseBranch()) {
                    // common case: JVM mode, or image-building initialization
                    final ResultHandle mccConfig = ccIsNotImage.invokeStaticMethod(createAndRegisterConfig);
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
                final String address = configDefinition.getAddressOfInstance(obj);
                if (address == null) return null;
                final FieldDescriptor descriptor = fields.get(address);
                if (descriptor == null) throw new IllegalStateException("No field for config address " + address);
                return body.readStaticField(descriptor);
            }
        }));

        runTimeInitConsumer.accept(new RuntimeReinitializedClassBuildItem(CONFIG_HELPER));
    }

    private void writeParsing(final ClassCreator cc, final BytecodeCreator body, final ResultHandle config, final ConfigPatternMap<LeafConfigType> keyMap) {
        // setup
        // Iterable iterable = config.getPropertyNames();
        final ResultHandle iterable = body.invokeVirtualMethod(MethodDescriptor.ofMethod(SmallRyeConfig.class, "getPropertyNames", Iterable.class), config);
        // Iterator iterator = iterable.iterator();
        final ResultHandle iterator = body.invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), iterable);

        // loop: {
        try (BytecodeCreator loop = body.createScope()) {
            // if (iterator.hasNext())
            final BranchResult ifHasNext = loop.ifNonZero(loop.invokeInterfaceMethod(ITR_HAS_NEXT, iterator));
            // {
            try (BytecodeCreator hasNext = ifHasNext.trueBranch()) {
                // key = iterator.next();
                final ResultHandle key = hasNext.checkCast(hasNext.invokeInterfaceMethod(ITR_NEXT, iterator), String.class);
                // NameIterator keyIter = new NameIterator(key);
                final ResultHandle keyIter = hasNext.newInstance(MethodDescriptor.ofConstructor(NameIterator.class, String.class), key);
                // if (! keyIter.hasNext()) continue loop;
                hasNext.ifNonZero(hasNext.invokeVirtualMethod(NI_HAS_NEXT, keyIter)).falseBranch().continueScope(loop);
                // if (! keyIter.nextSegmentEquals("shamrock")) continue loop;
                hasNext.ifNonZero(hasNext.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, hasNext.load("shamrock"))).falseBranch().continueScope(loop);
                // keyIter.next(); // skip "shamrock"
                hasNext.invokeVirtualMethod(NI_NEXT, keyIter);
                // parse(config, keyIter);
                hasNext.invokeStaticMethod(generateParserBody(cc, keyMap, new StringBuilder("parseKey"), new HashMap<>()), config, keyIter);
                // continue loop;
                hasNext.continueScope(loop);
            }
            // }
        }
        // }
        body.returnValue(body.loadNull());
    }

    private MethodDescriptor generateParserBody(final ClassCreator cc, final ConfigPatternMap<LeafConfigType> keyMap, final StringBuilder methodName, final Map<String, MethodDescriptor> parseMethodCache) {
        final String methodNameStr = methodName.toString();
        final MethodDescriptor existing = parseMethodCache.get(methodNameStr);
        if (existing != null) return existing;
        try (MethodCreator body = cc.getMethodCreator(methodName.toString(), void.class, SmallRyeConfig.class, NameIterator.class)) {
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
                if (name.equals(ConfigPatternMap.WC_SINGLE) || name.equals(ConfigPatternMap.WC_MULTI)) {
                    // skip
                } else {
                    // TODO: string switch
                    // if (keyIter.nextSegmentEquals(name)) {
                    try (BytecodeCreator nameMatched = body.ifNonZero(body.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, body.load(name))).trueBranch()) {
                        // keyIter.next();
                        nameMatched.invokeVirtualMethod(NI_NEXT, keyIter);
                        // (generated recursive)
                        final int length = methodName.length();
                        methodName.append('_').append(name);
                        nameMatched.invokeStaticMethod(generateParserBody(cc, keyMap.getChild(name), methodName, parseMethodCache), config, keyIter);
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
