package io.quarkus.deployment.configuration;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;
import static io.smallrye.config.common.utils.StringUtil.skewer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.ValueRegistryConfigSource;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.configuration.AbstractConfigBuilder;
import io.quarkus.runtime.configuration.ConfigDiagnostic;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.runtime.configuration.PropertiesUtil;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 *
 */
public final class RunTimeConfigurationGenerator {
    public static final String CONFIG_CLASS_NAME = "io.quarkus.runtime.generated.Config";
    public static final String CONFIG_STATIC_NAME = "io.quarkus.runtime.generated.StaticInitConfig";
    public static final String CONFIG_RUNTIME_NAME = "io.quarkus.runtime.generated.RunTimeConfig";

    public static final MethodDescriptor C_CREATE_RUN_TIME_CONFIG = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
            "createRunTimeConfig", void.class, ValueRegistry.class);
    public static final MethodDescriptor C_ENSURE_INITIALIZED = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME,
            "ensureInitialized", void.class);
    public static final MethodDescriptor REINIT = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "reinit",
            void.class);
    public static final MethodDescriptor C_READ_CONFIG = MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "readConfig", void.class,
            ValueRegistry.class);

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

    static final MethodDescriptor ITRA_ITERATOR = MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class);
    static final MethodDescriptor ITR_HAS_NEXT = MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class);
    static final MethodDescriptor ITR_NEXT = MethodDescriptor.ofMethod(Iterator.class, "next", Object.class);

    static final MethodDescriptor NI_NEW_STRING = MethodDescriptor.ofConstructor(NameIterator.class, String.class);
    static final MethodDescriptor NI_HAS_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    static final MethodDescriptor NI_NEXT = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);

    static final MethodDescriptor OBJ_TO_STRING = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    static final MethodDescriptor SB_NEW = MethodDescriptor.ofConstructor(StringBuilder.class);
    static final MethodDescriptor SB_APPEND_STRING = MethodDescriptor.ofMethod(StringBuilder.class, "append",
            StringBuilder.class, String.class);

    static final MethodDescriptor QCF_SET_CONFIG = MethodDescriptor.ofMethod(QuarkusConfigFactory.class, "setConfig",
            void.class, SmallRyeConfig.class);

    static final MethodDescriptor SRC_GET_PROPERTY_NAMES = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getPropertyNames",
            Iterable.class);

    static final MethodDescriptor SRCB_NEW = MethodDescriptor.ofConstructor(SmallRyeConfigBuilder.class);
    static final MethodDescriptor SRCB_WITH_CUSTOMIZER = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
            "withCustomizer", void.class, SmallRyeConfigBuilder.class, SmallRyeConfigBuilderCustomizer.class);
    static final MethodDescriptor SRCB_WITH_CUSTOMIZER_BY_NAME = MethodDescriptor.ofMethod(AbstractConfigBuilder.class,
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
    static final MethodDescriptor HS_ADD = MethodDescriptor.ofMethod(HashSet.class, "add", boolean.class, Object.class);

    private RunTimeConfigurationGenerator() {
    }

    public static final class GenerateOperation implements AutoCloseable {
        final boolean liveReloadPossible;
        final BuildTimeConfigurationReader.ReadResult buildTimeConfigResult;
        final ClassOutput classOutput;
        final ClassCreator cc;
        final MethodCreator clinit;
        final MethodCreator reinit;
        final MethodCreator readConfig;
        final ResultHandle clinitConfig;

        GenerateOperation(Builder builder) {
            liveReloadPossible = builder.liveReloadPossible;
            buildTimeConfigResult = Assert.checkNotNullParam("buildTimeReadResult", builder.getBuildTimeReadResult());
            classOutput = Assert.checkNotNullParam("classOutput", builder.getClassOutput());
            cc = ClassCreator.builder().classOutput(classOutput).className(CONFIG_CLASS_NAME).setFinal(true).build();
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

            // the build time config, which is for user use only (not used by us other than for loading converters)
            ResultHandle buildTimeBuilder = clinit.newInstance(SRCB_NEW);

            // static config builder
            clinit.invokeStaticMethod(SRCB_WITH_CUSTOMIZER_BY_NAME, buildTimeBuilder, clinit.load(CONFIG_STATIC_NAME));

            clinitConfig = clinit.checkCast(clinit.invokeVirtualMethod(SRCB_BUILD, buildTimeBuilder), SmallRyeConfig.class);

            // create readConfig
            readConfig = cc.getMethodCreator(C_READ_CONFIG);
        }

        public void run() {
            // in clinit, load the build-time config
            // make the build time config global until we read the run time config -
            // at run time (when we're ready) we update the factory and then release the build time config
            installConfiguration(clinitConfig, clinit);
            if (liveReloadPossible) {
                // the build time config, which is for user use only (not used by us other than for loading converters)
                ResultHandle buildTimeBuilder = reinit.newInstance(SRCB_NEW);
                // static config builder
                reinit.invokeStaticMethod(SRCB_WITH_CUSTOMIZER_BY_NAME, buildTimeBuilder, reinit.load(CONFIG_STATIC_NAME));

                ResultHandle clinitConfig = reinit.checkCast(reinit.invokeVirtualMethod(SRCB_BUILD, buildTimeBuilder),
                        SmallRyeConfig.class);
                installConfiguration(clinitConfig, reinit);
                reinit.returnValue(null);
            }

            // create the run time config
            ResultHandle runTimeBuilder = readConfig.newInstance(SRCB_NEW);
            ResultHandle valueRegistry = readConfig.getMethodParam(0);

            // config source for ValueRegistry
            MethodDescriptor valueRegistryConfigCustomizer = MethodDescriptor.ofMethod(ValueRegistryConfigSource.class,
                    "customizer", SmallRyeConfigBuilderCustomizer.class, ValueRegistry.class);
            ResultHandle customizer = readConfig.invokeStaticMethod(valueRegistryConfigCustomizer, valueRegistry);
            readConfig.invokeStaticMethod(SRCB_WITH_CUSTOMIZER, runTimeBuilder, customizer);

            // runtime config builder
            readConfig.invokeStaticMethod(SRCB_WITH_CUSTOMIZER_BY_NAME, runTimeBuilder, readConfig.load(CONFIG_RUNTIME_NAME));

            ResultHandle runTimeConfig = readConfig.invokeVirtualMethod(SRCB_BUILD, runTimeBuilder);
            installConfiguration(runTimeConfig, readConfig);

            // generate sweep for clinit
            configSweepLoop(clinit, clinitConfig, getRegisteredRoots(BUILD_AND_RUN_TIME_FIXED), Type.BUILD_TIME);
            clinit.invokeStaticMethod(CD_REPORT_UNKNOWN, clinit.readStaticField(C_UNKNOWN));

            if (liveReloadPossible) {
                configSweepLoop(readConfig, runTimeConfig, getRegisteredRoots(RUN_TIME), Type.RUNTIME);
            }
            // generate sweep for run time
            configSweepLoop(readConfig, runTimeConfig, getRegisteredRoots(RUN_TIME), Type.RUNTIME);
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
                mc.invokeVirtualMethod(C_READ_CONFIG, instance, mc.getMethodParam(0));
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

        private void configSweepLoop(MethodCreator method, ResultHandle config, Set<String> registeredRoots, Type type) {
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
                    ResultHandle unknown = type == Type.BUILD_TIME ? hasNext.readStaticField(C_UNKNOWN)
                            : hasNext.readStaticField(C_UNKNOWN_RUNTIME);
                    hasNext.invokeVirtualMethod(HS_ADD, unknown, key);

                    hasNext.continueScope(sweepLoop);
                }
            }
        }

        private Set<String> getRegisteredRoots(ConfigPhase configPhase) {
            Set<String> registeredRoots = new HashSet<>();
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
            private ClassOutput classOutput;
            private BuildTimeConfigurationReader.ReadResult buildTimeReadResult;

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

            public GenerateOperation build() {
                return new GenerateOperation(this);
            }
        }
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
