package io.quarkus.deployment.configuration;

import static io.quarkus.gizmo.MethodDescriptor.*;
import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;
import static io.smallrye.config.common.utils.StringUtil.skewer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
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
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 *
 */
public final class RunTimeConfigurationGenerator {
    public static final String CONFIG_CLASS_NAME = "io.quarkus.runtime.generated.Config";
    public static final String CONFIG_STATIC_NAME = "io.quarkus.runtime.generated.StaticInitConfig";
    public static final String CONFIG_RUNTIME_NAME = "io.quarkus.runtime.generated.RunTimeConfig";

    public static final MethodDescriptor C_STATIC_INIT_CONFIG = ofMethod(CONFIG_CLASS_NAME,
            "staticInitConfig", void.class);
    public static final MethodDescriptor C_RUN_TIME_CONFIG = ofMethod(CONFIG_CLASS_NAME,
            "runtimeConfig", void.class, ValueRegistry.class);

    static final MethodDescriptor CD_IS_ERROR = ofMethod(ConfigDiagnostic.class,
            "isError", boolean.class);
    static final MethodDescriptor CD_GET_ERROR_KEYS = ofMethod(ConfigDiagnostic.class,
            "getErrorKeys", Set.class);
    static final MethodDescriptor CD_RESET_ERROR = ofMethod(ConfigDiagnostic.class,
            "resetError", void.class);
    static final MethodDescriptor CD_REPORT_UNKNOWN = ofMethod(ConfigDiagnostic.class,
            "reportUnknown", void.class, Set.class);
    static final MethodDescriptor CD_REPORT_UNKNOWN_RUNTIME = ofMethod(ConfigDiagnostic.class,
            "reportUnknownRuntime", void.class, Set.class);

    static final MethodDescriptor ITRA_ITERATOR = ofMethod(Iterable.class, "iterator", Iterator.class);
    static final MethodDescriptor ITR_HAS_NEXT = ofMethod(Iterator.class, "hasNext", boolean.class);
    static final MethodDescriptor ITR_NEXT = ofMethod(Iterator.class, "next", Object.class);

    static final MethodDescriptor NI_NEW_STRING = ofConstructor(NameIterator.class, String.class);
    static final MethodDescriptor NI_HAS_NEXT = ofMethod(NameIterator.class, "hasNext", boolean.class);
    static final MethodDescriptor NI_NEXT = ofMethod(NameIterator.class, "next", void.class);

    static final MethodDescriptor OBJ_TO_STRING = ofMethod(Object.class, "toString", String.class);

    static final MethodDescriptor SB_NEW = ofConstructor(StringBuilder.class);
    static final MethodDescriptor SB_APPEND_STRING = ofMethod(StringBuilder.class,
            "append", StringBuilder.class, String.class);

    static final MethodDescriptor QCF_SET_CONFIG = ofMethod(QuarkusConfigFactory.class,
            "setConfig", void.class, SmallRyeConfig.class);

    static final MethodDescriptor SRC_GET_PROPERTY_NAMES = ofMethod(SmallRyeConfig.class,
            "getPropertyNames", Iterable.class);

    static final MethodDescriptor SRCB_NEW = ofConstructor(SmallRyeConfigBuilder.class);
    static final MethodDescriptor SRCB_WITH_CUSTOMIZER = ofMethod(AbstractConfigBuilder.class,
            "withCustomizer", void.class, SmallRyeConfigBuilder.class, SmallRyeConfigBuilderCustomizer.class);
    static final MethodDescriptor SRCB_WITH_CUSTOMIZER_BY_NAME = ofMethod(AbstractConfigBuilder.class,
            "withCustomizer", void.class, SmallRyeConfigBuilder.class, String.class);
    static final MethodDescriptor SRCB_BUILD = ofMethod(SmallRyeConfigBuilder.class,
            "build", SmallRyeConfig.class);

    static final MethodDescriptor PU_IS_MAPPED = ofMethod(PropertiesUtil.class,
            "isMapped", boolean.class, NameIterator.class, String.class);
    static final MethodDescriptor PU_IS_PROPERTY_QUARKUS_COMPOUND_NAME = ofMethod(PropertiesUtil.class,
            "isPropertyQuarkusCompoundName", boolean.class, NameIterator.class);
    static final MethodDescriptor PU_IS_PROPERTY_IN_ROOTS = ofMethod(PropertiesUtil.class,
            "isPropertyInRoots", boolean.class, String.class, Set.class);

    static final MethodDescriptor HS_NEW = ofConstructor(HashSet.class);
    static final MethodDescriptor HS_ADD = ofMethod(HashSet.class, "add", boolean.class, Object.class);

    static final MethodDescriptor VR_CUSTOMIZER = ofMethod(ValueRegistryConfigSource.class,
            "customizer", SmallRyeConfigBuilderCustomizer.class, ValueRegistry.class);

    private RunTimeConfigurationGenerator() {
    }

    public static final class GenerateOperation {
        final LaunchMode launchMode;
        final BuildTimeConfigurationReader.ReadResult buildTimeConfigResult;
        final ClassOutput classOutput;
        final ClassCreator cc;

        GenerateOperation(Builder builder) {
            launchMode = builder.getLaunchMode();
            buildTimeConfigResult = Assert.checkNotNullParam("buildTimeReadResult", builder.getBuildTimeReadResult());
            classOutput = Assert.checkNotNullParam("classOutput", builder.getClassOutput());
            cc = ClassCreator.builder().classOutput(classOutput).className(CONFIG_CLASS_NAME).setFinal(true).build();

            // Directly set the config resolver to avoid service loader. Test and Dev must set the resolver, so we cannot override it
            if (!launchMode.isDevOrTest()) {
                MethodCreator clinit = cc
                        .getMethodCreator(MethodDescriptor.ofMethod(CONFIG_CLASS_NAME, "<clinit>", void.class));
                clinit.setModifiers(Opcodes.ACC_STATIC);

                MethodDescriptor setInstance = ofMethod(ConfigProviderResolver.class,
                        "setInstance", void.class, ConfigProviderResolver.class);
                clinit.invokeStaticMethod(setInstance,
                        clinit.newInstance(ofConstructor(SmallRyeConfigProviderResolver.class)));

                clinit.returnValue(null);
                clinit.close();
            }

            // Private Constructor
            try (MethodCreator mc = cc.getMethodCreator(ofConstructor(CONFIG_CLASS_NAME))) {
                mc.setModifiers(Opcodes.ACC_PRIVATE);
                mc.invokeSpecialMethod(ofConstructor(Object.class), mc.getThis());
                mc.returnValue(null);
            }

            staticInitConfig();
            runtimeConfig();
            isMapped();

            cc.close();
        }

        private void staticInitConfig() {
            MethodCreator mc = cc.getMethodCreator(C_STATIC_INIT_CONFIG);
            mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

            // Set unknown = new HashSet();
            ResultHandle unknownSet = mc.newInstance(HS_NEW);
            // SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
            ResultHandle configBuilder = mc.newInstance(SRCB_NEW);
            // AbstractConfigBuilder.withCustomizer(builder, "io.quarkus.runtime.generated.StaticInitConfig");
            mc.invokeStaticMethod(SRCB_WITH_CUSTOMIZER_BY_NAME, configBuilder, mc.load(CONFIG_STATIC_NAME));
            //SmallRyeConfig config = builder.build();
            ResultHandle config = mc.invokeVirtualMethod(SRCB_BUILD, configBuilder);
            // QuarkusConfigFactory.setConfig(var1);
            installConfiguration(config, mc);

            // generate sweep for clinit
            configSweepLoop(mc, config, getRegisteredRoots(BUILD_AND_RUN_TIME_FIXED), unknownSet);
            mc.invokeStaticMethod(CD_REPORT_UNKNOWN, unknownSet);

            mc.returnValue(null);
            mc.close();
        }

        private void runtimeConfig() {
            MethodCreator mc = cc.getMethodCreator(C_RUN_TIME_CONFIG);
            mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

            // Set unknown = new HashSet();
            ResultHandle unknownSet = mc.newInstance(HS_NEW);
            // SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
            ResultHandle configBuilder = mc.newInstance(SRCB_NEW);

            // ValueRegistry parameter valueRegistry
            ResultHandle valueRegistry = mc.getMethodParam(0);
            // ConfigSource for ValueRegistry
            // SmallRyeConfigBuilderCustomizer customizer = ValueRegistryConfigSource.customizer(valueRegistry);
            ResultHandle customizer = mc.invokeStaticMethod(VR_CUSTOMIZER, valueRegistry);
            // AbstractConfigBuilder.withCustomizer(builder, customizer);
            mc.invokeStaticMethod(SRCB_WITH_CUSTOMIZER, configBuilder, customizer);
            // AbstractConfigBuilder.withCustomizer(builder, "io.quarkus.runtime.generated.RunTimeConfig");
            mc.invokeStaticMethod(SRCB_WITH_CUSTOMIZER_BY_NAME, configBuilder, mc.load(CONFIG_RUNTIME_NAME));
            //SmallRyeConfig config = builder.build();
            ResultHandle config = mc.invokeVirtualMethod(SRCB_BUILD, configBuilder);
            // QuarkusConfigFactory.setConfig(var1);
            installConfiguration(config, mc);

            // generate sweep for clinit
            configSweepLoop(mc, config, getRegisteredRoots(RUN_TIME), unknownSet);
            mc.invokeStaticMethod(CD_REPORT_UNKNOWN_RUNTIME, unknownSet);

            final BytecodeCreator isError = mc.ifNonZero(mc.invokeStaticMethod(CD_IS_ERROR)).trueBranch();
            ResultHandle niceErrorMessage = isError
                    .invokeStaticMethod(
                            ofMethod(ConfigDiagnostic.class, "getNiceErrorMessage", String.class));
            ResultHandle errorKeys = isError.invokeStaticMethod(CD_GET_ERROR_KEYS);
            isError.invokeStaticMethod(CD_RESET_ERROR);

            // throw the proper exception
            final ResultHandle finalErrorMessageBuilder = isError.newInstance(SB_NEW);
            isError.invokeVirtualMethod(SB_APPEND_STRING, finalErrorMessageBuilder, isError
                    .load("One or more configuration errors have prevented the application from starting. The errors are:\n"));
            isError.invokeVirtualMethod(SB_APPEND_STRING, finalErrorMessageBuilder, niceErrorMessage);
            final ResultHandle finalErrorMessage = isError.invokeVirtualMethod(OBJ_TO_STRING, finalErrorMessageBuilder);
            final ResultHandle configurationException = isError
                    .newInstance(ofConstructor(ConfigurationException.class, String.class, Set.class),
                            finalErrorMessage, errorKeys);
            final ResultHandle emptyStackTraceElement = isError.newArray(StackTraceElement.class, 0);
            // empty out the stack trace in order to not make the configuration errors more visible (the stack trace contains generated classes anyway that don't provide any value)
            isError.invokeVirtualMethod(
                    ofMethod(ConfigurationException.class, "setStackTrace", void.class,
                            StackTraceElement[].class),
                    configurationException, emptyStackTraceElement);
            isError.throwException(configurationException);

            mc.returnValue(null);
            mc.close();
        }

        private void configSweepLoop(MethodCreator method, ResultHandle config, Set<String> registeredRoots,
                ResultHandle unknown) {
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
                            ofMethod(CONFIG_CLASS_NAME, "isMapped", boolean.class, NameIterator.class),
                            keyIter);
                    try (BytecodeCreator isMappedPropertyTrue = hasNext.ifTrue(isMappedName).trueBranch()) {
                        isMappedPropertyTrue.continueScope(sweepLoop);
                    }

                    // if (PropertiesUtil.isPropertyQuarkusCompoundName(keyIter))
                    BranchResult quarkusCompoundName = hasNext
                            .ifNonZero(hasNext.invokeStaticMethod(PU_IS_PROPERTY_QUARKUS_COMPOUND_NAME, keyIter));
                    try (BytecodeCreator trueBranch = quarkusCompoundName.trueBranch()) {
                        trueBranch.invokeVirtualMethod(HS_ADD, unknown, key);
                    }

                    hasNext.ifNonZero(hasNext.invokeStaticMethod(PU_IS_PROPERTY_IN_ROOTS, key, rootSet)).falseBranch()
                            .continueScope(sweepLoop);
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

        private void isMapped() {
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
            isMapped("isMapped", patterns);
        }

        private void isMapped(final String methodName, final ConfigPatternMap<Boolean> names) {
            MethodDescriptor method = ofMethod(CONFIG_CLASS_NAME, methodName, boolean.class,
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
                            isMapped(childMethodName, child);
                            nextEqualsTrue.invokeVirtualMethod(NI_NEXT, nameIterator);
                            nextEqualsTrue
                                    .returnValue(nextEqualsTrue.invokeStaticMethod(ofMethod(CONFIG_CLASS_NAME,
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

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private LaunchMode launchMode;
            private ClassOutput classOutput;
            private BuildTimeConfigurationReader.ReadResult buildTimeReadResult;

            Builder() {
            }

            public LaunchMode getLaunchMode() {
                return launchMode;
            }

            public Builder setLaunchMode(LaunchMode launchMode) {
                this.launchMode = launchMode;
                return this;
            }

            ClassOutput getClassOutput() {
                return classOutput;
            }

            public Builder setClassOutput(final ClassOutput classOutput) {
                this.classOutput = classOutput;
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
}
