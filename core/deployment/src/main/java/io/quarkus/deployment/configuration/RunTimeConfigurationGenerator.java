package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.util.ReflectUtil.reportError;
import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;
import static io.smallrye.config.common.utils.StringUtil.skewer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.Converter;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.GroupDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.deployment.configuration.matching.Container;
import io.quarkus.deployment.configuration.matching.FieldContainer;
import io.quarkus.deployment.configuration.matching.MapContainer;
import io.quarkus.deployment.configuration.type.ArrayOf;
import io.quarkus.deployment.configuration.type.CollectionOf;
import io.quarkus.deployment.configuration.type.ConverterType;
import io.quarkus.deployment.configuration.type.Leaf;
import io.quarkus.deployment.configuration.type.LowerBoundCheckOf;
import io.quarkus.deployment.configuration.type.MinMaxValidated;
import io.quarkus.deployment.configuration.type.OptionalOf;
import io.quarkus.deployment.configuration.type.PatternValidated;
import io.quarkus.deployment.configuration.type.UpperBoundCheckOf;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.configuration.AbstractConfigBuilder;
import io.quarkus.runtime.configuration.ConfigDiagnostic;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.configuration.HyphenateEnumConverter;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.runtime.configuration.PropertiesUtil;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.Converters;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public final class RunTimeConfigurationGenerator {

    public static final String CONFIG_CLASS_NAME = "io.quarkus.runtime.generated.Config";
    public static final String CONFIG_STATIC_NAME = "io.quarkus.runtime.generated.StaticInitConfig";
    public static final String CONFIG_RUNTIME_NAME = "io.quarkus.runtime.generated.RunTimeConfig";

    public static final MethodDescriptor C_CREATE_RUN_TIME_CONFIG = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
            "createRunTimeConfig", void.class);
    public static final MethodDescriptor C_ENSURE_INITIALIZED = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
            "ensureInitialized", void.class);
    public static final MethodDescriptor REINIT = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "reinit",
            void.class);
    public static final MethodDescriptor C_READ_CONFIG = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "readConfig", void.class);

    static final FieldDescriptor C_UNKNOWN = FieldDescriptor.of(CONFIG_CLASS_NAME, "unknown", Set.class);
    static final FieldDescriptor C_UNKNOWN_RUNTIME = FieldDescriptor.of(CONFIG_CLASS_NAME, "unknownRuntime", Set.class);

    static final MethodDescriptor CD_INVALID_VALUE = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "invalidValue",
            void.class, String.class, IllegalArgumentException.class);
    static final MethodDescriptor CD_IS_ERROR = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "isError",
            boolean.class);
    static final MethodDescriptor CD_GET_ERROR_KEYS = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "getErrorKeys",
            Set.class);
    static final MethodDescriptor CD_MISSING_VALUE = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "missingValue",
            void.class, String.class, NoSuchElementException.class);
    static final MethodDescriptor CD_RESET_ERROR = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "resetError", void.class);
    static final MethodDescriptor CD_REPORT_UNKNOWN = MethodDescriptor.ofMethod(ConfigDiagnostic.class, "reportUnknown",
            void.class, Set.class);
    static final MethodDescriptor CD_REPORT_UNKNOWN_RUNTIME = MethodDescriptor.ofMethod(ConfigDiagnostic.class,
            "reportUnknownRuntime", void.class, Set.class);

    static final MethodDescriptor CONVS_NEW_ARRAY_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "newArrayConverter", Converter.class, Converter.class, Class.class);
    static final MethodDescriptor CONVS_NEW_COLLECTION_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "newCollectionConverter", Converter.class, Converter.class, IntFunction.class);
    static final MethodDescriptor CONVS_NEW_OPTIONAL_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "newOptionalConverter", Converter.class, Converter.class);
    static final MethodDescriptor CONVS_RANGE_VALUE_STRING_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "rangeValueStringConverter", Converter.class, Converter.class, String.class, boolean.class, String.class,
            boolean.class);
    static final MethodDescriptor CONVS_MINIMUM_VALUE_STRING_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "minimumValueStringConverter", Converter.class, Converter.class, String.class, boolean.class);
    static final MethodDescriptor CONVS_MAXIMUM_VALUE_STRING_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "maximumValueStringConverter", Converter.class, Converter.class, String.class, boolean.class);
    static final MethodDescriptor CONVS_PATTERN_CONVERTER = MethodDescriptor.ofMethod(Converters.class,
            "patternConverter", Converter.class, Converter.class, Pattern.class);

    static final MethodDescriptor CU_LIST_FACTORY = MethodDescriptor.ofMethod(ConfigUtils.class, "listFactory",
            IntFunction.class);
    static final MethodDescriptor CU_SET_FACTORY = MethodDescriptor.ofMethod(ConfigUtils.class, "setFactory",
            IntFunction.class);
    static final MethodDescriptor CU_SORTED_SET_FACTORY = MethodDescriptor.ofMethod(ConfigUtils.class, "sortedSetFactory",
            IntFunction.class);
    static final MethodDescriptor ITRA_ITERATOR = MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class);

    static final MethodDescriptor ITR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);
    static final MethodDescriptor ITR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);

    static final MethodDescriptor MAP_GET = MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class);
    static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class,
            Object.class);

    static final MethodDescriptor NI_GET_ALL_PREVIOUS_SEGMENTS = MethodDescriptor.ofMethod(NameIterator.class,
            "getAllPreviousSegments", String.class);
    static final MethodDescriptor NI_GET_NAME = MethodDescriptor.ofMethod(NameIterator.class, "getName", String.class);
    static final MethodDescriptor NI_GET_PREVIOUS_SEGMENT = MethodDescriptor.ofMethod(NameIterator.class, "getPreviousSegment",
            String.class);
    static final MethodDescriptor NI_HAS_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    static final MethodDescriptor NI_NEW_STRING = MethodDescriptor.ofConstructor(NameIterator.class, String.class);
    static final MethodDescriptor NI_NEXT_EQUALS = MethodDescriptor.ofMethod(NameIterator.class, "nextSegmentEquals",
            boolean.class, String.class);
    static final MethodDescriptor NI_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);
    static final MethodDescriptor NI_PREVIOUS = MethodDescriptor.ofMethod(NameIterator.class, "previous", void.class);
    static final MethodDescriptor NI_GO_TO_START = MethodDescriptor.ofMethod(NameIterator.class, "goToStart", void.class);

    static final MethodDescriptor OBJ_TO_STRING = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    static final MethodDescriptor OPT_EMPTY = MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class);
    static final MethodDescriptor OPT_GET = MethodDescriptor.ofMethod(Optional.class, "get", Object.class);
    static final MethodDescriptor OPT_IS_PRESENT = MethodDescriptor.ofMethod(Optional.class, "isPresent", boolean.class);
    static final MethodDescriptor OPT_OF = MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class);

    static final MethodDescriptor SB_NEW = MethodDescriptor.ofConstructor(StringBuilder.class);
    static final MethodDescriptor SB_NEW_STR = MethodDescriptor.ofConstructor(StringBuilder.class, String.class);
    static final MethodDescriptor SB_APPEND_STRING = MethodDescriptor.ofMethod(StringBuilder.class, "append",
            StringBuilder.class, String.class);
    static final MethodDescriptor SB_APPEND_CHAR = MethodDescriptor.ofMethod(StringBuilder.class, "append",
            StringBuilder.class, char.class);
    static final MethodDescriptor SB_LENGTH = MethodDescriptor.ofMethod(StringBuilder.class, "length",
            int.class);
    static final MethodDescriptor SB_SET_LENGTH = MethodDescriptor.ofMethod(StringBuilder.class, "setLength",
            void.class, int.class);

    static final MethodDescriptor QCF_SET_CONFIG = MethodDescriptor.ofMethod(QuarkusConfigFactory.class, "setConfig",
            void.class, SmallRyeConfig.class);

    static final MethodDescriptor SRC_GET_CONVERTER = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getConverter",
            Converter.class, Class.class);
    static final MethodDescriptor SRC_GET_PROPERTY_NAMES = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getPropertyNames",
            Iterable.class);
    static final MethodDescriptor SRC_GET_VALUE = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValue",
            Object.class, String.class, Converter.class);

    static final MethodDescriptor SRCB_NEW = MethodDescriptor.ofConstructor(SmallRyeConfigBuilder.class);
    static final MethodDescriptor SRCB_WITH_CONVERTER = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
            "withConverter", ConfigBuilder.class, Class.class, int.class, Converter.class);
    static final MethodDescriptor SRCB_WITH_CUSTOMIZER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withCustomizer", void.class, SmallRyeConfigBuilder.class, String.class);
    static final MethodDescriptor SRCB_BUILD = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class, "build",
            SmallRyeConfig.class);

    static final MethodDescriptor PU_IS_MAPPED = MethodDescriptor.ofMethod(PropertiesUtil.class, "isMapped", boolean.class,
            NameIterator.class, String.class);
    static final MethodDescriptor PU_IS_PROPERTY_QUARKUS_COMPOUND_NAME = MethodDescriptor.ofMethod(PropertiesUtil.class,
            "isPropertyQuarkusCompoundName", boolean.class, NameIterator.class);
    static final MethodDescriptor PU_IS_PROPERTY_IN_ROOTS = MethodDescriptor.ofMethod(PropertiesUtil.class, "isPropertyInRoots",
            boolean.class, String.class, Set.class);

    static final MethodDescriptor HS_NEW = MethodDescriptor.ofConstructor(HashSet.class);
    static final MethodDescriptor HS_NEW_SIZED = MethodDescriptor.ofConstructor(HashSet.class, int.class);
    static final MethodDescriptor HS_ADD = MethodDescriptor.ofMethod(HashSet.class, "add", boolean.class, Object.class);

    // todo: more space-efficient sorted map impl
    static final MethodDescriptor TM_NEW = MethodDescriptor.ofConstructor(TreeMap.class);

    static final MethodDescriptor EMPTY_PARSER = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "emptyParseKey", void.class,
            SmallRyeConfig.class, NameIterator.class);
    static final MethodDescriptor RT_EMPTY_PARSER = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "rtEmptyParseKey", void.class,
            SmallRyeConfig.class, NameIterator.class);

    private RunTimeConfigurationGenerator() {
    }

    public static final class GenerateOperation implements AutoCloseable {
        final boolean liveReloadPossible;
        final LaunchMode launchMode;
        final AccessorFinder accessorFinder;
        final ClassOutput classOutput;
        final ClassCreator cc;
        final MethodCreator clinit;
        final MethodCreator reinit;
        final BytecodeCreator converterSetup;
        final MethodCreator readConfig;
        final ResultHandle readConfigNameBuilder;
        final ResultHandle clinitNameBuilder;
        final BuildTimeConfigurationReader.ReadResult buildTimeConfigResult;
        final List<RootDefinition> roots;
        final Map<Container, MethodDescriptor> enclosingMemberMethods = new HashMap<>();
        final Map<Class<?>, MethodDescriptor> groupInitMethods = new HashMap<>();
        final Map<Class<?>, FieldDescriptor> configRootsByType = new HashMap<>();
        final ResultHandle clinitConfig;
        final Map<FieldDescriptor, Class<?>> convertersToRegister = new HashMap<>();
        final List<Class<?>> additionalTypes;
        /**
         * Regular converters organized by type. Each converter is stored in a separate field. Some are used
         * only at build time, some only at run time, and some at both times.
         * Producing a native image will automatically delete the converters which are not used at run time from the
         * final image.
         */
        final Map<ConverterType, FieldDescriptor> convertersByType = new HashMap<>();
        /**
         * Cache of things created in `clinit` which are then stored in fields, including config roots and converter
         * instances. The result handles are usable only from `clinit`.
         */
        final Map<FieldDescriptor, ResultHandle> instanceCache = new HashMap<>();
        /**
         * Converter fields have numeric names to keep space down.
         */
        int converterIndex = 0;

        GenerateOperation(Builder builder) {
            this.launchMode = builder.launchMode;
            this.liveReloadPossible = builder.liveReloadPossible;
            final BuildTimeConfigurationReader.ReadResult buildTimeReadResult = builder.buildTimeReadResult;
            buildTimeConfigResult = Assert.checkNotNullParam("buildTimeReadResult", buildTimeReadResult);
            classOutput = Assert.checkNotNullParam("classOutput", builder.getClassOutput());
            roots = Assert.checkNotNullParam("builder.roots", builder.getBuildTimeReadResult().getAllRoots());
            additionalTypes = Assert.checkNotNullParam("additionalTypes", builder.getAdditionalTypes());
            cc = ClassCreator.builder().classOutput(classOutput).className(CONFIG_CLASS_NAME).setFinal(true).build();
            generateEmptyParsers();
            // not instantiable
            try (MethodCreator mc = cc.getMethodCreator(MethodDescriptor.ofConstructor(CONFIG_CLASS_NAME))) {
                mc.setModifiers(Opcodes.ACC_PRIVATE);
                mc.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), mc.getThis());
                mc.returnValue(null);
            }
            if (liveReloadPossible) {
                reinit = cc.getMethodCreator(REINIT);
                reinit.setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC);
            } else {
                reinit = null;
            }

            // create <clinit>
            clinit = cc.getMethodCreator(MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "<clinit>", void.class));
            clinit.setModifiers(Opcodes.ACC_STATIC);

            cc.getFieldCreator(C_UNKNOWN).setModifiers(Opcodes.ACC_STATIC);
            clinit.writeStaticField(C_UNKNOWN, clinit.newInstance(HS_NEW));

            cc.getFieldCreator(C_UNKNOWN_RUNTIME).setModifiers(Opcodes.ACC_STATIC);
            clinit.writeStaticField(C_UNKNOWN_RUNTIME, clinit.newInstance(HS_NEW));

            generateIsMapped();

            clinitNameBuilder = clinit.newInstance(SB_NEW);

            // the build time config, which is for user use only (not used by us other than for loading converters)
            ResultHandle buildTimeBuilder = clinit.newInstance(SRCB_NEW);

            // static config builder
            clinit.invokeStaticMethod(SRCB_WITH_CUSTOMIZER, buildTimeBuilder, clinit.load(CONFIG_STATIC_NAME));

            clinitConfig = clinit.checkCast(clinit.invokeVirtualMethod(SRCB_BUILD, buildTimeBuilder), SmallRyeConfig.class);

            // block for converter setup
            converterSetup = clinit.createScope();

            // create readConfig
            readConfig = cc.getMethodCreator(C_READ_CONFIG);
            // the readConfig name builder
            readConfigNameBuilder = readConfig.newInstance(SB_NEW);

            accessorFinder = new AccessorFinder();
        }

        public void run() {
            // in clinit, load the build-time config
            // make the build time config global until we read the run time config -
            // at run time (when we're ready) we update the factory and then release the build time config
            installConfiguration(clinitConfig, clinit);
            if (liveReloadPossible) {
                // the build time config, which is for user use only (not used by us other than for loading converters)
                final ResultHandle buildTimeBuilder = reinit.newInstance(SRCB_NEW);
                // static config builder
                reinit.invokeStaticMethod(SRCB_WITH_CUSTOMIZER, buildTimeBuilder, reinit.load(CONFIG_STATIC_NAME));

                ResultHandle clinitConfig = reinit.checkCast(reinit.invokeVirtualMethod(SRCB_BUILD, buildTimeBuilder),
                        SmallRyeConfig.class);
                installConfiguration(clinitConfig, reinit);
                reinit.returnValue(null);
            }

            // fill roots map
            for (RootDefinition root : roots) {
                configRootsByType.put(root.getConfigurationClass(), root.getDescriptor());
            }

            // generate the parse methods and populate converters

            final ConfigPatternMap<Container> buildTimePatternMap = buildTimeConfigResult.getBuildTimePatternMap();
            final ConfigPatternMap<Container> buildTimeRunTimePatternMap = buildTimeConfigResult
                    .getBuildTimeRunTimePatternMap();
            final ConfigPatternMap<Container> runTimePatternMap = buildTimeConfigResult.getRunTimePatternMap();

            final BiFunction<Container, Container, Container> combinator = (a, b) -> a == null ? b : a;
            final ConfigPatternMap<Container> buildTimeRunTimeIgnored = ConfigPatternMap.merge(buildTimePatternMap,
                    runTimePatternMap, combinator);
            final ConfigPatternMap<Container> runTimeIgnored = ConfigPatternMap.merge(buildTimePatternMap,
                    buildTimeRunTimePatternMap, combinator);

            final MethodDescriptor siParserBody = generateParserBody(buildTimeRunTimePatternMap, buildTimeRunTimeIgnored,
                    new StringBuilder("siParseKey"), false, Type.BUILD_TIME);
            final MethodDescriptor rtParserBody = generateParserBody(runTimePatternMap, runTimeIgnored,
                    new StringBuilder("rtParseKey"), false, Type.RUNTIME);

            // create the run time config
            final ResultHandle runTimeBuilder = readConfig.newInstance(SRCB_NEW);

            // add in known converters
            for (Class<?> additionalType : additionalTypes) {
                ConverterType type = new Leaf(additionalType, null);
                FieldDescriptor fd = convertersByType.get(type);
                if (fd == null) {
                    // it's an unknown
                    final ResultHandle clazzHandle = converterSetup.loadClassFromTCCL(additionalType);
                    fd = FieldDescriptor.of(cc.getClassName(), "conv$" + converterIndex++, Converter.class);
                    ResultHandle converter = converterSetup.invokeVirtualMethod(SRC_GET_CONVERTER, clinitConfig, clazzHandle);
                    cc.getFieldCreator(fd).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
                    converterSetup.writeStaticField(fd, converter);
                    convertersByType.put(type, fd);
                    instanceCache.put(fd, converter);
                    convertersToRegister.put(fd, additionalType);
                }
            }
            if (!convertersToRegister.isEmpty()) {
                for (Map.Entry<FieldDescriptor, Class<?>> entry : convertersToRegister.entrySet()) {
                    final FieldDescriptor descriptor = entry.getKey();
                    final Class<?> type = entry.getValue();
                    readConfig.invokeVirtualMethod(SRCB_WITH_CONVERTER, runTimeBuilder, readConfig.loadClassFromTCCL(type),
                            readConfig.load(100), readConfig.readStaticField(descriptor));
                }
            }

            // runtime config builder
            readConfig.invokeStaticMethod(SRCB_WITH_CUSTOMIZER, runTimeBuilder, readConfig.load(CONFIG_RUNTIME_NAME));

            final ResultHandle runTimeConfig = readConfig.invokeVirtualMethod(SRCB_BUILD, runTimeBuilder);
            installConfiguration(runTimeConfig, readConfig);

            final ResultHandle clInitOldLen = clinit.invokeVirtualMethod(SB_LENGTH, clinitNameBuilder);
            final ResultHandle rcOldLen = readConfig.invokeVirtualMethod(SB_LENGTH, readConfigNameBuilder);

            // generate eager config read (both build and run time at once)
            for (RootDefinition root : roots) {
                // common things for all config phases
                final Class<?> configurationClass = root.getConfigurationClass();
                FieldDescriptor rootFieldDescriptor = root.getDescriptor();

                // Get or generate group init method
                MethodDescriptor initGroup = null;
                if (root.getConfigPhase() != ConfigPhase.BUILD_TIME) {
                    initGroup = generateInitGroup(root);
                }

                MethodDescriptor accessorCtor = null;
                if (!Modifier.isPublic(configurationClass.getModifiers())) {
                    accessorCtor = accessorFinder
                            .getConstructorFor(MethodDescriptor.ofConstructor(configurationClass));
                }

                // specific actions based on config phase
                if (root.getConfigPhase() == BUILD_AND_RUN_TIME_FIXED) {
                    // config root field is volatile in dev mode, final otherwise; we initialize it from clinit, and readConfig in dev mode
                    cc.getFieldCreator(rootFieldDescriptor)
                            .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
                                    | (liveReloadPossible ? Opcodes.ACC_VOLATILE : Opcodes.ACC_FINAL));

                    // construct instance in <clinit>
                    ResultHandle instance;
                    if (accessorCtor == null) {
                        instance = clinit.newInstance(MethodDescriptor.ofConstructor(configurationClass));
                    } else {
                        instance = clinit.invokeStaticMethod(accessorCtor);
                    }

                    // assign instance to field
                    clinit.writeStaticField(rootFieldDescriptor, instance);
                    instanceCache.put(rootFieldDescriptor, instance);
                    // eager init as appropriate
                    clinit.invokeVirtualMethod(SB_APPEND_STRING, clinitNameBuilder, clinit.load(root.getName()));
                    clinit.invokeStaticMethod(initGroup, clinitConfig, clinitNameBuilder, instance);
                    clinit.invokeVirtualMethod(SB_SET_LENGTH, clinitNameBuilder, clInitOldLen);
                    if (liveReloadPossible) {
                        instance = readConfig.readStaticField(rootFieldDescriptor);
                        readConfig.invokeVirtualMethod(SB_APPEND_STRING, readConfigNameBuilder,
                                readConfig.load(root.getName()));
                        readConfig.invokeStaticMethod(initGroup, runTimeConfig, readConfigNameBuilder, instance);
                        readConfig.invokeVirtualMethod(SB_SET_LENGTH, readConfigNameBuilder, rcOldLen);
                    }
                } else if (root.getConfigPhase() == ConfigPhase.RUN_TIME) {
                    // config root field is volatile; we initialize and read config from the readConfig method
                    cc.getFieldCreator(rootFieldDescriptor)
                            .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE);

                    // construct instance in readConfig
                    final ResultHandle instance;
                    if (accessorCtor == null) {
                        instance = readConfig.newInstance(MethodDescriptor.ofConstructor(configurationClass));
                    } else {
                        instance = readConfig.invokeStaticMethod(accessorCtor);
                    }

                    // assign instance to field
                    readConfig.writeStaticField(rootFieldDescriptor, instance);
                    readConfig.invokeVirtualMethod(SB_APPEND_STRING, readConfigNameBuilder, readConfig.load(root.getName()));
                    readConfig.invokeStaticMethod(initGroup, runTimeConfig, readConfigNameBuilder, instance);
                    readConfig.invokeVirtualMethod(SB_SET_LENGTH, readConfigNameBuilder, rcOldLen);
                } else {
                    assert root.getConfigPhase() == ConfigPhase.BUILD_TIME;
                    // ignore explicitly for now (no eager read for these)
                }
            }

            // generate sweep for clinit
            configSweepLoop(siParserBody, clinit, clinitConfig, getRegisteredRoots(BUILD_AND_RUN_TIME_FIXED), Type.BUILD_TIME);
            clinit.invokeStaticMethod(CD_REPORT_UNKNOWN, clinit.readStaticField(C_UNKNOWN));

            if (liveReloadPossible) {
                configSweepLoop(siParserBody, readConfig, runTimeConfig, getRegisteredRoots(RUN_TIME), Type.RUNTIME);
            }
            // generate sweep for run time
            configSweepLoop(rtParserBody, readConfig, runTimeConfig, getRegisteredRoots(RUN_TIME), Type.RUNTIME);
            readConfig.invokeStaticMethod(CD_REPORT_UNKNOWN_RUNTIME, readConfig.readStaticField(C_UNKNOWN_RUNTIME));

            // generate ensure-initialized method
            // the point of this method is simply to initialize the Config class
            // thus initializing the config infrastructure before anything requests it
            try (MethodCreator mc = cc.getMethodCreator(C_ENSURE_INITIALIZED)) {
                mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                mc.returnValue(null);
            }

            // generate run time entry point
            try (MethodCreator mc = cc.getMethodCreator(C_CREATE_RUN_TIME_CONFIG)) {
                mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(CONFIG_CLASS_NAME));
                mc.invokeVirtualMethod(C_READ_CONFIG, instance);
                mc.returnValue(null);
            }

            // wrap it up
            final BytecodeCreator isError = readConfig.ifNonZero(readConfig.invokeStaticMethod(CD_IS_ERROR)).trueBranch();
            ResultHandle niceErrorMessage = isError
                    .invokeStaticMethod(
                            MethodDescriptor.ofMethod(ConfigDiagnostic.class, "getNiceErrorMessage", String.class));
            ResultHandle errorKeys = isError.invokeStaticMethod(CD_GET_ERROR_KEYS);
            isError.invokeStaticMethod(CD_RESET_ERROR);

            // throw the proper exception
            final ResultHandle finalErrorMessageBuilder = isError.newInstance(SB_NEW);
            isError.invokeVirtualMethod(SB_APPEND_STRING, finalErrorMessageBuilder, isError
                    .load("One or more configuration errors have prevented the application from starting. The errors are:\n"));
            isError.invokeVirtualMethod(SB_APPEND_STRING, finalErrorMessageBuilder, niceErrorMessage);
            final ResultHandle finalErrorMessage = isError.invokeVirtualMethod(OBJ_TO_STRING, finalErrorMessageBuilder);
            final ResultHandle configurationException = isError
                    .newInstance(MethodDescriptor.ofConstructor(ConfigurationException.class, String.class, Set.class),
                            finalErrorMessage, errorKeys);
            final ResultHandle emptyStackTraceElement = isError.newArray(StackTraceElement.class, 0);
            // empty out the stack trace in order to not make the configuration errors more visible (the stack trace contains generated classes anyway that don't provide any value)
            isError.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(ConfigurationException.class, "setStackTrace", void.class,
                            StackTraceElement[].class),
                    configurationException, emptyStackTraceElement);
            isError.throwException(configurationException);

            readConfig.returnValue(null);
            readConfig.close();

            clinit.returnValue(null);
            clinit.close();
            cc.close();
        }

        private void configSweepLoop(MethodDescriptor parserBody, MethodCreator method, ResultHandle config,
                Set<String> registeredRoots, Type type) {
            ResultHandle propertyNames = method.invokeVirtualMethod(SRC_GET_PROPERTY_NAMES, config);
            ResultHandle iterator = method.invokeInterfaceMethod(ITRA_ITERATOR, propertyNames);

            ResultHandle rootSet = method.newInstance(HS_NEW);
            for (String registeredRoot : registeredRoots) {
                method.invokeVirtualMethod(HS_ADD, rootSet, method.load(registeredRoot));
            }

            try (BytecodeCreator sweepLoop = method.createScope()) {
                try (BytecodeCreator hasNext = sweepLoop.ifNonZero(sweepLoop.invokeInterfaceMethod(ITR_HAS_NEXT, iterator))
                        .trueBranch()) {
                    ResultHandle key = hasNext.checkCast(hasNext.invokeInterfaceMethod(ITR_NEXT, iterator), String.class);

                    // NameIterator keyIter = new NameIterator(key);
                    ResultHandle keyIter = hasNext.newInstance(NI_NEW_STRING, key);
                    // if (!isMappedProperty(keyIter))
                    ResultHandle isMappedName = hasNext.invokeStaticMethod(
                            MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "isMapped", boolean.class, NameIterator.class),
                            keyIter);
                    try (BytecodeCreator isMappedPropertyTrue = hasNext.ifTrue(isMappedName).trueBranch()) {
                        isMappedPropertyTrue.continueScope(sweepLoop);
                    }

                    // keyIter.goToStart() - reset the NameIterator so it can be used by the ConfigRoot code
                    hasNext.invokeVirtualMethod(NI_GO_TO_START, keyIter);

                    // if (PropertiesUtil.isPropertyQuarkusCompoundName(keyIter))
                    BranchResult quarkusCompoundName = hasNext
                            .ifNonZero(hasNext.invokeStaticMethod(PU_IS_PROPERTY_QUARKUS_COMPOUND_NAME, keyIter));
                    try (BytecodeCreator trueBranch = quarkusCompoundName.trueBranch()) {
                        ResultHandle unknown = type == Type.BUILD_TIME ? trueBranch.readStaticField(C_UNKNOWN)
                                : trueBranch.readStaticField(C_UNKNOWN_RUNTIME);
                        trueBranch.invokeVirtualMethod(HS_ADD, unknown, key);
                    }

                    hasNext.ifNonZero(hasNext.invokeStaticMethod(PU_IS_PROPERTY_IN_ROOTS, key, rootSet)).falseBranch()
                            .continueScope(sweepLoop);

                    // if (! keyIter.hasNext()) continue sweepLoop;
                    hasNext.ifNonZero(hasNext.invokeVirtualMethod(NI_HAS_NEXT, keyIter)).falseBranch().continueScope(sweepLoop);
                    // parse(config, keyIter);
                    hasNext.invokeStaticMethod(parserBody, config, keyIter);
                    // continue sweepLoop;
                    hasNext.continueScope(sweepLoop);
                }
            }
        }

        private Set<String> getRegisteredRoots(ConfigPhase configPhase) {
            Set<String> registeredRoots = new HashSet<>();
            for (RootDefinition root : roots) {
                if (root.getConfigPhase().equals(configPhase)) {
                    registeredRoots.add(root.getName());
                }
            }
            if (BUILD_AND_RUN_TIME_FIXED.equals(configPhase)) {
                for (ConfigClass mapping : buildTimeConfigResult.getBuildTimeRunTimeMappings()) {
                    registeredRoots.add(mapping.getPrefix());
                }
            }
            if (RUN_TIME.equals(configPhase)) {
                for (ConfigClass mapping : buildTimeConfigResult.getRunTimeMappings()) {
                    registeredRoots.add(mapping.getPrefix());
                }
            }
            return registeredRoots;
        }

        private void installConfiguration(ResultHandle config, MethodCreator methodCreator) {
            methodCreator.invokeStaticMethod(QCF_SET_CONFIG, config);
        }

        private MethodDescriptor generateInitGroup(ClassDefinition definition) {
            final Class<?> clazz = definition.getConfigurationClass();
            MethodDescriptor methodDescriptor = groupInitMethods.get(clazz);
            if (methodDescriptor != null) {
                return methodDescriptor;
            }
            methodDescriptor = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "initGroup$" + clazz.getName().replace('.', '$'),
                    void.class, SmallRyeConfig.class, StringBuilder.class, Object.class);
            final MethodCreator bc = cc.getMethodCreator(methodDescriptor).setModifiers(Opcodes.ACC_STATIC);
            final ResultHandle config = bc.getMethodParam(0);
            // on entry, nameBuilder is our name
            final ResultHandle nameBuilder = bc.getMethodParam(1);
            final ResultHandle instance = bc.getMethodParam(2);
            final ResultHandle length = bc.invokeVirtualMethod(SB_LENGTH, nameBuilder);
            for (ClassDefinition.ClassMember member : definition.getMembers()) {
                // common setup
                final String propertyName = member.getPropertyName();
                MethodDescriptor setter = null; // we won't need a setter if the field is public
                if (!isFieldEligibleForDirectAccess(member)) {
                    setter = accessorFinder.getSetterFor(member.getDescriptor());
                }
                if (!propertyName.isEmpty()) {
                    // append the property name
                    bc.invokeVirtualMethod(SB_APPEND_CHAR, nameBuilder, bc.load('.'));
                    bc.invokeVirtualMethod(SB_APPEND_STRING, nameBuilder, bc.load(propertyName));
                }
                if (member instanceof ClassDefinition.ItemMember) {
                    ClassDefinition.ItemMember leafMember = (ClassDefinition.ItemMember) member;
                    final FieldDescriptor convField = getOrCreateConverterInstance(leafMember.getField());
                    final ResultHandle name = bc.invokeVirtualMethod(OBJ_TO_STRING, nameBuilder);
                    final ResultHandle converter = bc.readStaticField(convField);
                    try (TryBlock tryBlock = bc.tryBlock()) {
                        final ResultHandle val = tryBlock.invokeVirtualMethod(SRC_GET_VALUE, config, name, converter);
                        if (setter == null) {
                            tryBlock.writeInstanceField(member.getDescriptor(), instance, val);
                        } else {
                            tryBlock.invokeStaticMethod(setter, instance, val);
                        }
                        try (CatchBlockCreator catchBadValue = tryBlock.addCatch(IllegalArgumentException.class)) {
                            catchBadValue.invokeStaticMethod(CD_INVALID_VALUE, name, catchBadValue.getCaughtException());
                        }
                        try (CatchBlockCreator catchNoValue = tryBlock.addCatch(NoSuchElementException.class)) {
                            catchNoValue.invokeStaticMethod(CD_MISSING_VALUE, name, catchNoValue.getCaughtException());
                        }
                    }
                } else if (member instanceof ClassDefinition.GroupMember) {
                    ClassDefinition.GroupMember groupMember = (ClassDefinition.GroupMember) member;
                    if (groupMember.isOptional()) {
                        final ResultHandle val = bc.invokeStaticMethod(OPT_EMPTY);
                        if (setter == null) {
                            bc.writeInstanceField(member.getDescriptor(), instance, val);
                        } else {
                            bc.invokeStaticMethod(setter, instance, val);
                        }
                    } else {
                        final GroupDefinition groupDefinition = groupMember.getGroupDefinition();
                        final MethodDescriptor nested = generateInitGroup(groupDefinition);

                        final ResultHandle nestedInstance;
                        if (Modifier.isPublic(groupDefinition.getConfigurationClass().getModifiers())) {
                            nestedInstance = bc
                                    .newInstance(MethodDescriptor.ofConstructor(groupDefinition.getConfigurationClass()));
                        } else {
                            final MethodDescriptor ctor = accessorFinder
                                    .getConstructorFor(MethodDescriptor.ofConstructor(groupDefinition.getConfigurationClass()));
                            nestedInstance = bc.invokeStaticMethod(ctor);
                        }

                        bc.invokeStaticMethod(nested, config, nameBuilder, nestedInstance);
                        if (setter == null) {
                            bc.writeInstanceField(member.getDescriptor(), instance, nestedInstance);
                        } else {
                            bc.invokeStaticMethod(setter, instance, nestedInstance);
                        }
                    }
                } else {
                    assert member instanceof ClassDefinition.MapMember;
                    final ResultHandle map = bc.newInstance(TM_NEW);
                    if (setter == null) {
                        bc.writeInstanceField(member.getDescriptor(), instance, map);
                    } else {
                        bc.invokeStaticMethod(setter, instance, map);
                    }
                }
                if (!propertyName.isEmpty()) {
                    // restore length
                    bc.invokeVirtualMethod(SB_SET_LENGTH, nameBuilder, length);
                }
            }
            bc.returnValue(null);
            groupInitMethods.put(clazz, methodDescriptor);
            return methodDescriptor;
        }

        private void generateEmptyParsers() {
            MethodCreator body = cc.getMethodCreator(RT_EMPTY_PARSER);
            body.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            ResultHandle keyIter = body.getMethodParam(1);
            try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter))
                    .falseBranch()) {
                // return;
                matchedBody.returnValue(null);
            }
            reportUnknownRuntime(body, keyIter);
            body.returnValue(null);

            body = cc.getMethodCreator(EMPTY_PARSER);
            body.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            keyIter = body.getMethodParam(1);
            try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter))
                    .falseBranch()) {
                // return;
                matchedBody.returnValue(null);
            }
            reportUnknown(body, keyIter);
            body.returnValue(null);
        }

        private MethodDescriptor generateParserBody(final ConfigPatternMap<Container> keyMap,
                final ConfigPatternMap<?> ignoredMap, final StringBuilder methodName, final boolean dynamic,
                final Type type) {
            final Container matched = keyMap == null ? null : keyMap.getMatched();
            final Object ignoreMatched = ignoredMap == null ? null : ignoredMap.getMatched();

            if (matched == null && ignoreMatched != null) {
                //if this method is an ignored leaf node then we can avoid generating a method
                //this reduces the number of generated methods by close too 50%
                if (keyMap == null || !keyMap.childNames().iterator().hasNext()) {
                    final Iterable<String> names = ignoredMap.childNames();
                    boolean needsCode = false;
                    for (String name : names) {
                        if (name.equals(ConfigPatternMap.WILD_CARD)) {
                            needsCode = true;
                            break;
                        } else {
                            final ConfigPatternMap<Container> keyChildMap = keyMap == null ? null : keyMap.getChild(name);
                            if (keyChildMap == null) {
                                needsCode = true;
                                break;
                            }
                        }
                    }
                    if (!needsCode) {
                        return (type == Type.BUILD_TIME) ? EMPTY_PARSER : RT_EMPTY_PARSER;
                    }
                }
            }

            try (MethodCreator body = cc.getMethodCreator(methodName.toString(), void.class,
                    SmallRyeConfig.class, NameIterator.class)) {
                body.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
                final ResultHandle config = body.getMethodParam(0);
                final ResultHandle keyIter = body.getMethodParam(1);
                // if (! keyIter.hasNext()) {
                try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter))
                        .falseBranch()) {
                    if (matched != null) {
                        final ClassDefinition.ClassMember member = matched.getClassMember();
                        // matched members *must* be item members
                        assert member instanceof ClassDefinition.ItemMember;
                        ClassDefinition.ItemMember itemMember = (ClassDefinition.ItemMember) member;

                        if (matched instanceof FieldContainer) {
                            final FieldContainer fieldContainer = (FieldContainer) matched;
                            if (dynamic) {
                                generateConsumeSegments(matchedBody, keyIter, itemMember.getPropertyName());
                                // we have to get or create all containing (and contained) groups of this member
                                matchedBody.invokeStaticMethod(generateGetEnclosing(fieldContainer, type), keyIter,
                                        config);
                            }
                            // else ignore (already populated eagerly)
                        } else {
                            assert matched instanceof MapContainer;
                            MapContainer mapContainer = (MapContainer) matched;
                            // map leafs are always dynamic
                            final ResultHandle lastSeg = matchedBody.invokeVirtualMethod(NI_GET_PREVIOUS_SEGMENT, keyIter);
                            matchedBody.invokeVirtualMethod(NI_PREVIOUS, keyIter);
                            final ResultHandle mapHandle = matchedBody
                                    .invokeStaticMethod(generateGetEnclosing(mapContainer, type), keyIter, config);
                            // populate the map
                            final Field field = mapContainer.findField();
                            final FieldDescriptor fd = getOrCreateConverterInstance(field);
                            final ResultHandle key = matchedBody.invokeVirtualMethod(NI_GET_NAME, keyIter);
                            final ResultHandle converter = matchedBody.readStaticField(fd);
                            final ResultHandle value = matchedBody.invokeVirtualMethod(SRC_GET_VALUE, config, key, converter);
                            matchedBody.invokeInterfaceMethod(MAP_PUT, mapHandle, lastSeg, value);
                        }
                    } else if (ignoreMatched == null) {
                        // name is unknown
                        if (type == Type.BUILD_TIME) {
                            reportUnknown(matchedBody, keyIter);
                        } else {
                            reportUnknownRuntime(matchedBody, keyIter);
                        }
                    }
                    // return;
                    matchedBody.returnValue(null);
                }
                // }
                boolean hasWildCard = false;
                // branches for each next-string
                if (keyMap != null) {
                    final Iterable<String> names = keyMap.childNames();
                    for (String name : names) {
                        if (name.equals(ConfigPatternMap.WILD_CARD)) {
                            hasWildCard = true;
                        } else {
                            // TODO: string switch
                            // if (keyIter.nextSegmentEquals(name)) {
                            try (BytecodeCreator nameMatched = body
                                    .ifNonZero(body.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, body.load(name)))
                                    .trueBranch()) {
                                // keyIter.next();
                                nameMatched.invokeVirtualMethod(NI_NEXT, keyIter);
                                // (generated recursive)
                                final int length = methodName.length();
                                methodName.append(':').append(name);
                                nameMatched.invokeStaticMethod(
                                        generateParserBody(keyMap.getChild(name),
                                                ignoredMap == null ? null : ignoredMap.getChild(name), methodName, dynamic,
                                                type),
                                        config, keyIter);
                                methodName.setLength(length);
                                // return;
                                nameMatched.returnValue(null);
                            }
                            // }
                        }
                    }
                }
                // branches for each ignored child
                if (ignoredMap != null) {
                    final Iterable<String> names = ignoredMap.childNames();
                    for (String name : names) {
                        if (name.equals(ConfigPatternMap.WILD_CARD)) {
                            hasWildCard = true;
                        } else {
                            final ConfigPatternMap<Container> keyChildMap = keyMap == null ? null : keyMap.getChild(name);
                            if (keyChildMap != null) {
                                // we already did this one
                                continue;
                            }
                            // TODO: string switch
                            // if (keyIter.nextSegmentEquals(name)) {
                            try (BytecodeCreator nameMatched = body
                                    .ifNonZero(body.invokeVirtualMethod(NI_NEXT_EQUALS, keyIter, body.load(name)))
                                    .trueBranch()) {
                                // keyIter.next();
                                nameMatched.invokeVirtualMethod(NI_NEXT, keyIter);
                                // (generated recursive)
                                final int length = methodName.length();
                                methodName.append(':').append(name);
                                nameMatched.invokeStaticMethod(
                                        generateParserBody(null, ignoredMap.getChild(name), methodName, false, type),
                                        config, keyIter);
                                methodName.setLength(length);
                                // return;
                                nameMatched.returnValue(null);
                            }
                            // }
                        }
                    }
                }
                if (hasWildCard) {
                    assert keyMap != null || ignoredMap != null;
                    // consume and parse
                    try (BytecodeCreator matchedBody = body.ifNonZero(body.invokeVirtualMethod(NI_HAS_NEXT, keyIter))
                            .trueBranch()) {
                        // keyIter.next();
                        matchedBody.invokeVirtualMethod(NI_NEXT, keyIter);
                        // (generated recursive)
                        final int length = methodName.length();
                        methodName.append(":*");
                        matchedBody.invokeStaticMethod(
                                generateParserBody(keyMap == null ? null : keyMap.getChild(ConfigPatternMap.WILD_CARD),
                                        ignoredMap == null ? null : ignoredMap.getChild(ConfigPatternMap.WILD_CARD),
                                        methodName,
                                        true, type),
                                config, keyIter);
                        methodName.setLength(length);
                        // return;
                        matchedBody.returnValue(null);
                    }
                }
                if (type == Type.BUILD_TIME) {
                    reportUnknown(body, keyIter);
                } else {
                    reportUnknownRuntime(body, keyIter);
                }
                body.returnValue(null);
                return body.getMethodDescriptor();
            }
        }

        private MethodDescriptor generateGetEnclosing(final FieldContainer matchNode, final Type type) {
            // name iterator cursor is placed BEFORE the field name on entry
            MethodDescriptor md = enclosingMemberMethods.get(matchNode);
            if (md != null) {
                return md;
            }
            md = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
                    type.methodPrefix + "GetEnclosing:" + matchNode.getCombinedName(), Object.class,
                    NameIterator.class, SmallRyeConfig.class);
            try (MethodCreator mc = cc.getMethodCreator(md)) {
                mc.setModifiers(Opcodes.ACC_STATIC);
                final ResultHandle keyIter = mc.getMethodParam(0);
                final ResultHandle config = mc.getMethodParam(1);
                final ClassDefinition.ClassMember member = matchNode.getClassMember();
                final Container parent = matchNode.getParent();
                if (parent == null) {
                    // it's a root
                    final RootDefinition definition = (RootDefinition) member.getEnclosingDefinition();
                    FieldDescriptor fieldDescriptor = configRootsByType.get(definition.getConfigurationClass());
                    assert fieldDescriptor != null : "Field descriptor defined for " + definition.getConfigurationClass();
                    mc.returnValue(mc.readStaticField(fieldDescriptor));
                } else if (parent instanceof FieldContainer) {
                    // get the parent
                    final FieldContainer fieldContainer = (FieldContainer) parent;
                    final ClassDefinition.ClassMember classMember = fieldContainer.getClassMember();
                    int consumedSegmentsCount = generateConsumeSegments(mc, keyIter, classMember.getPropertyName());
                    final ResultHandle enclosing = mc.invokeStaticMethod(generateGetEnclosing(fieldContainer, type),
                            keyIter, config);

                    final ResultHandle fieldVal;
                    if (isFieldEligibleForDirectAccess(classMember)) {
                        fieldVal = mc.readInstanceField(classMember.getDescriptor(), enclosing);
                    } else {
                        final MethodDescriptor getter = accessorFinder.getGetterFor(classMember.getDescriptor());
                        fieldVal = mc.invokeStaticMethod(getter, enclosing);
                    }

                    final AssignableResultHandle group = mc.createVariable(Object.class);
                    if (classMember instanceof ClassDefinition.GroupMember
                            && ((ClassDefinition.GroupMember) classMember).isOptional()) {
                        final BranchResult isPresent = mc.ifNonZero(mc.invokeVirtualMethod(OPT_IS_PRESENT, fieldVal));
                        final BytecodeCreator trueBranch = isPresent.trueBranch();
                        final BytecodeCreator falseBranch = isPresent.falseBranch();
                        // it already exists
                        trueBranch.assign(group, trueBranch.invokeVirtualMethod(OPT_GET, fieldVal));

                        // it doesn't exist, recreate it
                        final ResultHandle instance;
                        if (Modifier.isPublic(member.getEnclosingDefinition().getConfigurationClass().getModifiers())) {
                            instance = falseBranch.newInstance(
                                    MethodDescriptor.ofConstructor(member.getEnclosingDefinition().getConfigurationClass()));
                        } else {
                            final MethodDescriptor ctor = accessorFinder.getConstructorFor(
                                    MethodDescriptor.ofConstructor(member.getEnclosingDefinition().getConfigurationClass()));
                            instance = falseBranch.invokeStaticMethod(ctor);
                        }

                        final ResultHandle precedingKey = falseBranch.invokeVirtualMethod(NI_GET_ALL_PREVIOUS_SEGMENTS,
                                keyIter);
                        final ResultHandle nameBuilder = falseBranch.newInstance(SB_NEW_STR, precedingKey);
                        falseBranch.invokeStaticMethod(generateInitGroup(member.getEnclosingDefinition()), config, nameBuilder,
                                instance);
                        final ResultHandle val = falseBranch.invokeStaticMethod(OPT_OF, instance);
                        if (isFieldEligibleForDirectAccess(member)) {
                            falseBranch.writeInstanceField(member.getDescriptor(), instance, val);
                        } else {
                            final MethodDescriptor setter = accessorFinder.getSetterFor(classMember.getDescriptor());
                            falseBranch.invokeStaticMethod(setter, fieldVal, val);
                        }
                        falseBranch.assign(group, instance);
                    } else {
                        mc.assign(group, fieldVal);
                    }
                    generateRestoreSegments(mc, keyIter, consumedSegmentsCount);
                    mc.returnValue(group);
                } else {
                    assert parent instanceof MapContainer;
                    // the map might or might not contain this group
                    final MapContainer mapContainer = (MapContainer) parent;
                    final ResultHandle key = mc.invokeVirtualMethod(NI_GET_PREVIOUS_SEGMENT, keyIter);
                    // consume segment
                    mc.invokeVirtualMethod(NI_PREVIOUS, keyIter);
                    final ResultHandle map = mc.invokeStaticMethod(generateGetEnclosing(mapContainer, type), keyIter,
                            config);
                    // restore
                    mc.invokeVirtualMethod(NI_NEXT, keyIter);
                    final ResultHandle existing = mc.invokeInterfaceMethod(MAP_GET, map, key);
                    mc.ifNull(existing).falseBranch().returnValue(existing);

                    // add the map key and initialize the enclosed item
                    final ResultHandle instance;
                    if (Modifier.isPublic(member.getEnclosingDefinition().getConfigurationClass().getModifiers())) {
                        instance = mc.newInstance(
                                MethodDescriptor.ofConstructor(member.getEnclosingDefinition().getConfigurationClass()));
                    } else {
                        final MethodDescriptor ctor = accessorFinder.getConstructorFor(
                                MethodDescriptor.ofConstructor(member.getEnclosingDefinition().getConfigurationClass()));
                        instance = mc.invokeStaticMethod(ctor);
                    }

                    final ResultHandle precedingKey = mc.invokeVirtualMethod(NI_GET_ALL_PREVIOUS_SEGMENTS, keyIter);
                    final ResultHandle nameBuilder = mc.newInstance(SB_NEW_STR, precedingKey);
                    mc.invokeStaticMethod(generateInitGroup(member.getEnclosingDefinition()), config, nameBuilder, instance);
                    mc.invokeInterfaceMethod(MAP_PUT, map, key, instance);
                    mc.returnValue(instance);
                }
            }
            enclosingMemberMethods.put(matchNode, md);
            return md;
        }

        private MethodDescriptor generateGetEnclosing(final MapContainer matchNode, final Type type) {
            // name iterator cursor is placed BEFORE the map key on entry
            MethodDescriptor md = enclosingMemberMethods.get(matchNode);
            if (md != null) {
                return md;
            }
            md = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
                    type.methodPrefix + "GetEnclosing:" + matchNode.getCombinedName(), Object.class,
                    NameIterator.class, SmallRyeConfig.class);
            try (MethodCreator mc = cc.getMethodCreator(md)) {
                mc.setModifiers(Opcodes.ACC_STATIC);
                final ResultHandle keyIter = mc.getMethodParam(0);
                final ResultHandle config = mc.getMethodParam(1);
                final Container parent = matchNode.getParent();
                if (parent instanceof FieldContainer) {
                    // get the parent
                    final FieldContainer fieldContainer = (FieldContainer) parent;
                    int consumedSegmentsCount = generateConsumeSegments(mc, keyIter,
                            fieldContainer.getClassMember().getPropertyName());
                    final ResultHandle enclosing = mc.invokeStaticMethod(generateGetEnclosing(fieldContainer, type),
                            keyIter, config);
                    generateRestoreSegments(mc, keyIter, consumedSegmentsCount);

                    final ResultHandle result;
                    if (isFieldEligibleForDirectAccess(fieldContainer.getClassMember())) {
                        result = mc.readInstanceField(fieldContainer.getClassMember().getDescriptor(), enclosing);
                    } else {
                        final MethodDescriptor getter = accessorFinder
                                .getGetterFor(fieldContainer.getClassMember().getDescriptor());
                        result = mc.invokeStaticMethod(getter, enclosing);
                    }
                    mc.returnValue(result);
                } else {
                    assert parent instanceof MapContainer;
                    // the map might or might not contain this map
                    final MapContainer mapContainer = (MapContainer) parent;
                    final ResultHandle key = mc.invokeVirtualMethod(NI_GET_PREVIOUS_SEGMENT, keyIter);
                    // consume enclosing map key
                    mc.invokeVirtualMethod(NI_PREVIOUS, keyIter);
                    final ResultHandle map = mc.invokeStaticMethod(generateGetEnclosing(mapContainer, type), keyIter,
                            config);
                    // restore
                    mc.invokeVirtualMethod(NI_NEXT, keyIter);
                    final ResultHandle existing = mc.invokeInterfaceMethod(MAP_GET, map, key);
                    mc.ifNull(existing).falseBranch().returnValue(existing);
                    // add the map key and initialize the enclosed item
                    final ResultHandle instance = mc.newInstance(TM_NEW);
                    mc.invokeInterfaceMethod(MAP_PUT, map, key, instance);
                    mc.returnValue(instance);
                }
            }
            enclosingMemberMethods.put(matchNode, md);
            return md;
        }

        private int generateConsumeSegments(BytecodeCreator bc, ResultHandle keyIter, String propertyName) {
            if (propertyName.isEmpty()) {
                return 0;
            }
            bc.invokeVirtualMethod(NI_PREVIOUS, keyIter);
            int consumedSegmentsCount = 1;
            // For properties whose name contains dots,
            // we need to call previous() multiple times
            int dotIndex = propertyName.indexOf('.');
            while (dotIndex >= 0) {
                bc.invokeVirtualMethod(NI_PREVIOUS, keyIter);
                ++consumedSegmentsCount;
                dotIndex = propertyName.indexOf('.', dotIndex + 1);
            }
            return consumedSegmentsCount;
        }

        private void generateRestoreSegments(BytecodeCreator bc, ResultHandle keyIter, int consumedSegmentsCount) {
            for (int i = 0; i < consumedSegmentsCount; i++) {
                bc.invokeVirtualMethod(NI_NEXT, keyIter);
            }
        }

        private FieldDescriptor getOrCreateConverterInstance(Field field) {
            return getOrCreateConverterInstance(field, ConverterType.of(field));
        }

        private FieldDescriptor getOrCreateConverterInstance(Field field, ConverterType type) {
            FieldDescriptor fd = convertersByType.get(type);
            if (fd != null) {
                return fd;
            }

            fd = FieldDescriptor.of(cc.getClassName(), "conv$" + converterIndex++, Converter.class);
            ResultHandle converter;
            boolean storeConverter = false;
            if (type instanceof Leaf) {
                // simple type
                final Leaf leaf = (Leaf) type;
                final Class<? extends Converter<?>> convertWith = leaf.getConvertWith();
                if (convertWith != null) {
                    // TODO: temporary until type param inference is in
                    if (convertWith == HyphenateEnumConverter.class.asSubclass(Converter.class)) {
                        converter = converterSetup.newInstance(MethodDescriptor.ofConstructor(convertWith, Class.class),
                                converterSetup.loadClassFromTCCL(type.getLeafType()));
                    } else {
                        converter = converterSetup.newInstance(MethodDescriptor.ofConstructor(convertWith));
                    }
                } else {
                    final ResultHandle clazzHandle = converterSetup.loadClassFromTCCL(leaf.getLeafType());
                    converter = converterSetup.invokeVirtualMethod(SRC_GET_CONVERTER, clinitConfig, clazzHandle);
                    storeConverter = true;
                }
            } else if (type instanceof ArrayOf) {
                final ArrayOf arrayOf = (ArrayOf) type;
                final ResultHandle nestedConv = instanceCache
                        .get(getOrCreateConverterInstance(field, arrayOf.getElementType()));
                converter = converterSetup.invokeStaticMethod(CONVS_NEW_ARRAY_CONVERTER, nestedConv,
                        converterSetup.loadClassFromTCCL(arrayOf.getArrayType()));
            } else if (type instanceof CollectionOf) {
                final CollectionOf collectionOf = (CollectionOf) type;
                final ResultHandle nestedConv = instanceCache
                        .get(getOrCreateConverterInstance(field, collectionOf.getElementType()));
                final ResultHandle factory;
                final Class<?> collectionClass = collectionOf.getCollectionClass();
                if (collectionClass == List.class) {
                    factory = converterSetup.invokeStaticMethod(CU_LIST_FACTORY);
                } else if (collectionClass == Set.class) {
                    factory = converterSetup.invokeStaticMethod(CU_SET_FACTORY);
                } else if (collectionClass == SortedSet.class) {
                    factory = converterSetup.invokeStaticMethod(CU_SORTED_SET_FACTORY);
                } else {
                    throw reportError(field, "Unsupported configuration collection type: %s", collectionClass);
                }
                converter = converterSetup.invokeStaticMethod(CONVS_NEW_COLLECTION_CONVERTER, nestedConv, factory);
            } else if (type instanceof LowerBoundCheckOf) {
                final LowerBoundCheckOf boundCheckOf = (LowerBoundCheckOf) type;
                // todo: add in bounds checker
                converter = instanceCache
                        .get(getOrCreateConverterInstance(field, boundCheckOf.getClassConverterType()));
            } else if (type instanceof UpperBoundCheckOf) {
                final UpperBoundCheckOf boundCheckOf = (UpperBoundCheckOf) type;
                // todo: add in bounds checker
                converter = instanceCache
                        .get(getOrCreateConverterInstance(field, boundCheckOf.getClassConverterType()));
            } else if (type instanceof MinMaxValidated) {
                MinMaxValidated minMaxValidated = (MinMaxValidated) type;
                String min = minMaxValidated.getMin();
                boolean minInclusive = minMaxValidated.isMinInclusive();
                String max = minMaxValidated.getMax();
                boolean maxInclusive = minMaxValidated.isMaxInclusive();
                final ResultHandle nestedConv = instanceCache
                        .get(getOrCreateConverterInstance(field, minMaxValidated.getNestedType()));
                if (min != null) {
                    if (max != null) {
                        converter = converterSetup.invokeStaticMethod(
                                CONVS_RANGE_VALUE_STRING_CONVERTER,
                                nestedConv,
                                converterSetup.load(min),
                                converterSetup.load(minInclusive),
                                converterSetup.load(max),
                                converterSetup.load(maxInclusive));
                    } else {
                        converter = converterSetup.invokeStaticMethod(
                                CONVS_MINIMUM_VALUE_STRING_CONVERTER,
                                nestedConv,
                                converterSetup.load(min),
                                converterSetup.load(minInclusive));
                    }
                } else {
                    assert min == null && max != null;
                    converter = converterSetup.invokeStaticMethod(
                            CONVS_MAXIMUM_VALUE_STRING_CONVERTER,
                            nestedConv,
                            converterSetup.load(max),
                            converterSetup.load(maxInclusive));
                }
            } else if (type instanceof OptionalOf) {
                OptionalOf optionalOf = (OptionalOf) type;
                final ResultHandle nestedConv = instanceCache
                        .get(getOrCreateConverterInstance(field, optionalOf.getNestedType()));
                converter = converterSetup.invokeStaticMethod(CONVS_NEW_OPTIONAL_CONVERTER, nestedConv);
            } else if (type instanceof PatternValidated) {
                PatternValidated patternValidated = (PatternValidated) type;
                final ResultHandle nestedConv = instanceCache
                        .get(getOrCreateConverterInstance(field, patternValidated.getNestedType()));
                final ResultHandle patternStr = converterSetup.load(patternValidated.getPatternString());
                converter = converterSetup.invokeStaticMethod(CONVS_PATTERN_CONVERTER, nestedConv, patternStr);
            } else {
                throw Assert.unreachableCode();
            }
            cc.getFieldCreator(fd).setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
            converterSetup.writeStaticField(fd, converter);
            convertersByType.put(type, fd);
            instanceCache.put(fd, converter);
            if (storeConverter) {
                convertersToRegister.put(fd, type.getLeafType());
            }
            return fd;
        }

        private void generateIsMapped() {
            ConfigPatternMap<Boolean> patterns = new ConfigPatternMap<>();
            List<ConfigClass> configMappings = buildTimeConfigResult.getAllMappings();
            for (ConfigClass configMapping : configMappings) {
                Set<String> names = ConfigMappings.getProperties(configMapping).keySet();
                for (String name : names) {
                    NameIterator ni = new NameIterator(name);
                    ConfigPatternMap<Boolean> current = patterns;
                    while (ni.hasNext()) {
                        String segment = ni.getNextSegment();
                        ConfigPatternMap<Boolean> child = current.getChild(segment);
                        if (child == null) {
                            child = new ConfigPatternMap<>();
                            current.addChild(segment, child);
                        }
                        current = child;
                        ni.next();
                    }
                    current.setMatched(true);
                }
            }
            generateIsMapped("isMapped", patterns);
        }

        private void generateIsMapped(final String methodName, final ConfigPatternMap<Boolean> names) {
            MethodDescriptor method = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, methodName, boolean.class,
                    NameIterator.class);
            MethodCreator mc = cc.getMethodCreator(method);
            mc.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);

            ResultHandle nameIterator = mc.getMethodParam(0);
            BranchResult hasNext = mc.ifTrue(mc.invokeVirtualMethod(NI_HAS_NEXT, nameIterator));

            try (BytecodeCreator hasNextTrue = hasNext.trueBranch()) {
                ArrayDeque<String> childNames = new ArrayDeque<>();
                // * matching has to come last
                for (String childName : names.childNames()) {
                    if (childName.startsWith("*")) {
                        childNames.addLast(childName);
                    } else {
                        childNames.addFirst(childName);
                    }
                }

                for (String childName : childNames) {
                    ConfigPatternMap<Boolean> child = names.getChild(childName);
                    BranchResult nextEquals = hasNextTrue
                            .ifTrue(hasNextTrue.invokeStaticMethod(PU_IS_MAPPED, nameIterator, hasNextTrue.load(childName)));
                    try (BytecodeCreator nextEqualsTrue = nextEquals.trueBranch()) {
                        childName = childName.replace("[*]", "-collection");
                        String childMethodName = methodName + "$" + skewer(childName, '_');
                        if (child.getMatched() == null) {
                            generateIsMapped(childMethodName, child);
                            nextEqualsTrue.invokeVirtualMethod(NI_NEXT, nameIterator);
                            nextEqualsTrue
                                    .returnValue(nextEqualsTrue.invokeStaticMethod(MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
                                            childMethodName, boolean.class, NameIterator.class), nameIterator));
                        } else {
                            nextEqualsTrue.returnBoolean(true);
                        }
                    }
                }
                hasNextTrue.returnBoolean(false);
            }

            try (BytecodeCreator hasNextFalse = hasNext.falseBranch()) {
                hasNextFalse.returnBoolean(false);
            }

            mc.returnBoolean(false);
            mc.close();
        }

        private void reportUnknown(BytecodeCreator bc, ResultHandle unknownProperty) {
            ResultHandle property = bc.invokeVirtualMethod(NI_GET_NAME, unknownProperty);
            ResultHandle unknown = bc.readStaticField(C_UNKNOWN);
            bc.invokeVirtualMethod(HS_ADD, unknown, property);
        }

        private void reportUnknownRuntime(BytecodeCreator bc, ResultHandle unknownProperty) {
            ResultHandle property = bc.invokeVirtualMethod(NI_GET_NAME, unknownProperty);
            ResultHandle unknown = bc.readStaticField(C_UNKNOWN_RUNTIME);
            bc.invokeVirtualMethod(HS_ADD, unknown, property);
        }

        public void close() {
            try {
                clinit.close();
            } catch (Throwable t) {
                try {
                    cc.close();
                } catch (Throwable t2) {
                    t2.addSuppressed(t);
                    throw t2;
                }
                throw t;
            }
            cc.close();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            public boolean liveReloadPossible;
            private LaunchMode launchMode;
            private ClassOutput classOutput;
            private BuildTimeConfigurationReader.ReadResult buildTimeReadResult;
            private List<Class<?>> additionalTypes;

            Builder() {
            }

            ClassOutput getClassOutput() {
                return classOutput;
            }

            public Builder setClassOutput(final ClassOutput classOutput) {
                this.classOutput = classOutput;
                return this;
            }

            public Builder setLiveReloadPossible(boolean liveReloadPossible) {
                this.liveReloadPossible = liveReloadPossible;
                return this;
            }

            BuildTimeConfigurationReader.ReadResult getBuildTimeReadResult() {
                return buildTimeReadResult;
            }

            public Builder setBuildTimeReadResult(final BuildTimeConfigurationReader.ReadResult buildTimeReadResult) {
                this.buildTimeReadResult = buildTimeReadResult;
                return this;
            }

            List<Class<?>> getAdditionalTypes() {
                return additionalTypes;
            }

            public Builder setAdditionalTypes(final List<Class<?>> additionalTypes) {
                this.additionalTypes = additionalTypes;
                return this;
            }

            public LaunchMode getLaunchMode() {
                return launchMode;
            }

            public Builder setLaunchMode(LaunchMode launchMode) {
                this.launchMode = launchMode;
                return this;
            }

            public GenerateOperation build() {
                return new GenerateOperation(this);
            }
        }
    }

    /**
     * A field is eligible for direct access if the following conditions are met
     * 1) the field is public
     * 2) the enclosing class is public
     * 3) the class type of the field is public
     */
    private static boolean isFieldEligibleForDirectAccess(ClassDefinition.ClassMember classMember) {
        return Modifier.isPublic(classMember.getField().getModifiers())
                && Modifier.isPublic(classMember.getEnclosingDefinition().getConfigurationClass().getModifiers())
                && Modifier.isPublic(classMember.getField().getType().getModifiers());
    }

    private enum Type {
        BUILD_TIME("si"),
        RUNTIME("rt");

        final String methodPrefix;

        Type(String methodPrefix) {
            this.methodPrefix = methodPrefix;
        }
    }
}
