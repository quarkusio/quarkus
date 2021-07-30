package io.quarkus.deployment.logging;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.NamedLogHandlersBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.WebSocketLogHandlerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.dev.testing.MessageFormat;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.dev.console.CurrentAppExceptionHighlighter;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.CategoryBuildTimeConfig;
import io.quarkus.runtime.logging.CleanupFilterConfig;
import io.quarkus.runtime.logging.InheritableLevel;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.logging.LogCleanupFilterElement;
import io.quarkus.runtime.logging.LogConfig;
import io.quarkus.runtime.logging.LogMetricsHandlerRecorder;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public final class LoggingResourceProcessor {

    private static final String LOGMANAGER_LOGGER_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logmanager_Logger";
    private static final String LOGGING_LOGGER_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logging_Logger";
    private static final String LOGGER_NODE_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logmanager_LoggerNode";

    private static final String MIN_LEVEL_COMPUTE_CLASS_NAME = "io.quarkus.runtime.generated.MinLevelCompute";
    private static final MethodDescriptor IS_MIN_LEVEL_ENABLED = MethodDescriptor.ofMethod(MIN_LEVEL_COMPUTE_CLASS_NAME,
            "isMinLevelEnabled",
            boolean.class, int.class, String.class);

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.jboss.threads", "JBoss Threads version"));
    }

    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BuildStep
    void setUpDefaultLevels(List<LogCategoryBuildItem> categories,
            Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCategoryBuildItem category : categories) {
            configOutput.accept(
                    new RunTimeConfigurationDefaultBuildItem(
                            "quarkus.log.category.\"" + category.getCategory() + "\".level",
                            category.getLevel().toString()));
        }
    }

    @BuildStep
    void setUpDefaultLogCleanupFilters(List<LogCleanupFilterBuildItem> logCleanupFilters,
            Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCleanupFilterBuildItem logCleanupFilter : logCleanupFilters) {
            String startsWithClause = logCleanupFilter.getFilterElement().getMessageStarts().stream()
                    // SmallRye Config escaping is pretty naive so we only need to escape commas
                    .map(s -> s.replace(",", "\\,"))
                    .collect(Collectors.joining(","));
            configOutput.accept(
                    new RunTimeConfigurationDefaultBuildItem(
                            "quarkus.log.filter.\"" + logCleanupFilter.getFilterElement().getLoggerName()
                                    + "\".if-starts-with",
                            startsWithClause));
            if (logCleanupFilter.getFilterElement().getTargetLevel() != null) {
                configOutput.accept(
                        new RunTimeConfigurationDefaultBuildItem(
                                "quarkus.log.filter.\"" + logCleanupFilter.getFilterElement().getLoggerName()
                                        + "\".target-level",
                                logCleanupFilter.getFilterElement().getTargetLevel().toString()));
            }
        }
    }

    @BuildStep
    void miscSetup(
            Consumer<RuntimeInitializedClassBuildItem> runtimeInit,
            Consumer<NativeImageSystemPropertyBuildItem> systemProp,
            Consumer<ServiceProviderBuildItem> provider) {
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("org.jboss.logmanager.formatters.TrueColorHolder"));
        systemProp
                .accept(new NativeImageSystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager"));
        provider.accept(
                new ServiceProviderBuildItem(EmbeddedConfigurator.class.getName(), InitialConfigurator.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LoggingSetupBuildItem setupLoggingRuntimeInit(LoggingSetupRecorder recorder, LogConfig log, LogBuildTimeConfig buildLog,
            ConsoleRuntimeConfig consoleRuntimeConfig,
            Optional<WebSocketLogHandlerBuildItem> logStreamHandlerBuildItem,
            List<LogHandlerBuildItem> handlerBuildItems,
            List<NamedLogHandlersBuildItem> namedHandlerBuildItems, List<LogConsoleFormatBuildItem> consoleFormatItems,
            Optional<ConsoleFormatterBannerBuildItem> possibleBannerBuildItem,
            List<LogStreamBuildItem> logStreamBuildItems,
            LaunchModeBuildItem launchModeBuildItem,
            List<LogCleanupFilterBuildItem> logCleanupFilters) {
        if (!launchModeBuildItem.isAuxiliaryApplication()
                || launchModeBuildItem.getAuxiliaryDevModeType().orElse(null) == DevModeType.TEST_ONLY) {
            final List<RuntimeValue<Optional<Handler>>> handlers = handlerBuildItems.stream()
                    .map(LogHandlerBuildItem::getHandlerValue)
                    .collect(Collectors.toList());
            final List<RuntimeValue<Map<String, Handler>>> namedHandlers = namedHandlerBuildItems.stream()
                    .map(NamedLogHandlersBuildItem::getNamedHandlersMap).collect(Collectors.toList());

            ConsoleFormatterBannerBuildItem bannerBuildItem = null;
            RuntimeValue<Optional<Supplier<String>>> possibleSupplier = null;
            if (possibleBannerBuildItem.isPresent()) {
                bannerBuildItem = possibleBannerBuildItem.get();
            }
            if (bannerBuildItem != null) {
                possibleSupplier = bannerBuildItem.getBannerSupplier();
            }
            // Dev UI Log Stream
            RuntimeValue<Optional<Handler>> devUiLogHandler = null;
            if (logStreamHandlerBuildItem.isPresent()) {
                devUiLogHandler = logStreamHandlerBuildItem.get().getHandlerValue();
            }
            boolean alwaysEnableLogStream = false;
            if (!logStreamBuildItems.isEmpty()) {
                alwaysEnableLogStream = true;
            }

            recorder.initializeLogging(log, buildLog, consoleRuntimeConfig, alwaysEnableLogStream, devUiLogHandler, handlers,
                    namedHandlers,
                    consoleFormatItems.stream().map(LogConsoleFormatBuildItem::getFormatterValue).collect(Collectors.toList()),
                    possibleSupplier, launchModeBuildItem.getLaunchMode());
            LogConfig logConfig = new LogConfig();
            ConfigInstantiator.handleObject(logConfig);
            for (LogCleanupFilterBuildItem i : logCleanupFilters) {
                CleanupFilterConfig value = new CleanupFilterConfig();
                LogCleanupFilterElement filterElement = i.getFilterElement();
                value.ifStartsWith = filterElement.getMessageStarts();
                value.targetLevel = filterElement.getTargetLevel() == null ? org.jboss.logmanager.Level.DEBUG
                        : filterElement.getTargetLevel();
                logConfig.filters.put(filterElement.getLoggerName(), value);
            }
            ConsoleRuntimeConfig crc = new ConsoleRuntimeConfig();
            ConfigInstantiator.handleObject(crc);
            LoggingSetupRecorder.initializeBuildTimeLogging(logConfig, buildLog, crc, launchModeBuildItem.getLaunchMode());
            ((QuarkusClassLoader) Thread.currentThread().getContextClassLoader()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    InitialConfigurator.DELAYED_HANDLER.buildTimeComplete();
                }
            });
        }
        return new LoggingSetupBuildItem();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(TestSetupBuildItem.class)
    @Produce(LogConsoleFormatBuildItem.class)
    void setupStackTraceFormatter(ApplicationArchivesBuildItem item) {
        List<IndexView> indexList = new ArrayList<>();
        for (ApplicationArchive i : item.getAllApplicationArchives()) {
            if (Files.isDirectory(i.getArchiveLocation())) {
                indexList.add(i.getIndex());
            }
        }
        CompositeIndex index = CompositeIndex.create(indexList);
        //awesome/horrible hack
        //we know from the index which classes are part of the current application
        //we add ANSI codes for bold and underline to their names to display them more prominently
        CurrentAppExceptionHighlighter.THROWABLE_FORMATTER = new BiConsumer<LogRecord, Consumer<LogRecord>>() {
            @Override
            public void accept(LogRecord logRecord, Consumer<LogRecord> logRecordConsumer) {
                Map<Throwable, StackTraceElement[]> restore = new HashMap<>();
                Throwable c = logRecord.getThrown();
                while (c != null) {
                    StackTraceElement[] stackTrace = c.getStackTrace();
                    for (int i = 0; i < stackTrace.length; ++i) {
                        var elem = stackTrace[i];
                        if (index.getClassByName(DotName.createSimple(elem.getClassName())) != null) {
                            stackTrace[i] = new StackTraceElement(elem.getClassLoaderName(), elem.getModuleName(),
                                    elem.getModuleVersion(),
                                    MessageFormat.UNDERLINE + MessageFormat.BOLD + elem.getClassName()
                                            + MessageFormat.NO_UNDERLINE + MessageFormat.NO_BOLD,
                                    elem.getMethodName(), elem.getFileName(), elem.getLineNumber());
                        }
                    }
                    restore.put(c, c.getStackTrace());
                    c.setStackTrace(stackTrace);
                    c = c.getCause();
                }
                try {
                    logRecordConsumer.accept(logRecord);
                } finally {
                    for (Map.Entry<Throwable, StackTraceElement[]> entry : restore.entrySet()) {
                        entry.getKey().setStackTrace(entry.getValue());
                    }
                }
            }
        };
        ((QuarkusClassLoader) getClass().getClassLoader()).addCloseTask(new Runnable() {
            @Override
            public void run() {
                CurrentAppExceptionHighlighter.THROWABLE_FORMATTER = null;
            }
        });
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupLoggingStaticInit(LoggingSetupRecorder recorder, LaunchModeBuildItem launchModeBuildItem) {
        if (!launchModeBuildItem.isAuxiliaryApplication()) {
            recorder.initializeLoggingForImageBuild();
        }
    }

    // This is specifically to help out with presentations, to allow an env var to always override this value
    @BuildStep
    void setUpDarkeningDefault(Consumer<RunTimeConfigurationDefaultBuildItem> rtcConsumer) {
        rtcConsumer.accept(new RunTimeConfigurationDefaultBuildItem("quarkus.log.console.darken", "0"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(LogMetricsHandlerRecorder recorder, LogBuildTimeConfig log,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            BuildProducer<LogHandlerBuildItem> logHandler, Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (metricsCapability.isPresent() && log.metricsEnabled) {
            recorder.initCounters();
            metrics.produce(new MetricsFactoryConsumerBuildItem(recorder.registerMetrics()));
            logHandler.produce(new LogHandlerBuildItem(recorder.getLogHandler()));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void setUpMinLevelLogging(LogBuildTimeConfig log,
            final BuildProducer<GeneratedClassBuildItem> generatedTraceLogger) {
        ClassOutput output = new GeneratedClassGizmoAdaptor(generatedTraceLogger, false);
        if (log.categories.isEmpty() || allMinLevelInfoOrHigher(log.minLevel.intValue(), log.categories)) {
            generateDefaultLoggers(log.minLevel, output);
        } else {
            generateCategoryMinLevelLoggers(log.categories, log.minLevel, output);
        }
    }

    private static boolean allMinLevelInfoOrHigher(int minLogLevel, Map<String, CategoryBuildTimeConfig> categories) {
        return categories.values().stream()
                .allMatch(categoryConfig -> categoryConfig.minLevel.getLevel().intValue() >= minLogLevel);
    }

    private static void generateDefaultLoggers(Level minLevel, ClassOutput output) {
        generateDefaultLoggingLogger(minLevel, output);
        generateDefaultLoggerNode(output);
        generateLogManagerLogger(output, LoggingResourceProcessor.generateMinLevelDefault(minLevel.getName()));
    }

    private static void generateCategoryMinLevelLoggers(Map<String, CategoryBuildTimeConfig> categories, Level rootMinLevel,
            ClassOutput output) {
        generateMinLevelCompute(categories, rootMinLevel, output);
        generateDefaultLoggerNode(output);
        generateLogManagerLogger(output, LoggingResourceProcessor::generateMinLevelCheckCategory);
    }

    private static BranchResult generateMinLevelCheckCategory(MethodCreator method, FieldDescriptor nameAliasDescriptor) {
        final ResultHandle levelIntValue = getParamLevelIntValue(method);
        final ResultHandle nameAlias = method.readInstanceField(nameAliasDescriptor, method.getThis());
        return method.ifTrue(method.invokeStaticMethod(IS_MIN_LEVEL_ENABLED, levelIntValue, nameAlias));
    }

    private static void generateMinLevelCompute(Map<String, CategoryBuildTimeConfig> categories, Level rootMinLevel,
            ClassOutput output) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className(MIN_LEVEL_COMPUTE_CLASS_NAME)
                .classOutput(output).build()) {

            try (MethodCreator mc = cc.getMethodCreator(IS_MIN_LEVEL_ENABLED)) {
                mc.setModifiers(Opcodes.ACC_STATIC);

                final ResultHandle level = mc.getMethodParam(0);
                final ResultHandle name = mc.getMethodParam(1);

                BytecodeCreator current = mc;
                for (Map.Entry<String, CategoryBuildTimeConfig> entry : categories.entrySet()) {
                    final String category = entry.getKey();
                    final int categoryLevelIntValue = getLogMinLevel(entry.getKey(), entry.getValue(), categories, rootMinLevel)
                            .intValue();

                    ResultHandle equalsResult = current.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(Object.class, "equals", boolean.class, Object.class),
                            name, current.load(category));

                    BranchResult equalsBranch = current.ifTrue(equalsResult);
                    try (BytecodeCreator false1 = equalsBranch.falseBranch()) {
                        ResultHandle startsWithResult = false1.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(String.class, "startsWith", boolean.class, String.class),
                                name, false1.load(category + "."));

                        BranchResult startsWithBranch = false1.ifTrue(startsWithResult);

                        final BytecodeCreator startsWithTrue = startsWithBranch.trueBranch();
                        final BranchResult levelCompareBranch = startsWithTrue.ifIntegerGreaterEqual(level,
                                startsWithTrue.load(categoryLevelIntValue));
                        levelCompareBranch.trueBranch().returnValue(levelCompareBranch.trueBranch().load(true));
                        levelCompareBranch.falseBranch().returnValue(levelCompareBranch.falseBranch().load(false));

                        current = startsWithBranch.falseBranch();
                    }

                    equalsBranch.trueBranch().returnValue(equalsBranch.trueBranch().load(true));
                }

                final ResultHandle infoLevelIntValue = getLogManagerLevelIntValue(rootMinLevel.toString(), current);
                final BranchResult isInfoOrHigherBranch = current.ifIntegerGreaterEqual(level, infoLevelIntValue);
                isInfoOrHigherBranch.trueBranch().returnValue(isInfoOrHigherBranch.trueBranch().load(true));
                isInfoOrHigherBranch.falseBranch().returnValue(isInfoOrHigherBranch.falseBranch().load(false));
            }
        }
    }

    private static Level getLogMinLevel(String categoryName, CategoryBuildTimeConfig categoryConfig,
            Map<String, CategoryBuildTimeConfig> categories,
            Level rootMinLevel) {
        if (Objects.isNull(categoryConfig))
            return rootMinLevel;

        final InheritableLevel inheritableLevel = categoryConfig.minLevel;
        if (!inheritableLevel.isInherited())
            return inheritableLevel.getLevel();

        int lastDotIndex = categoryName.lastIndexOf('.');
        if (lastDotIndex == -1)
            return rootMinLevel;

        String parent = categoryName.substring(0, lastDotIndex);
        return getLogMinLevel(parent, categories.get(parent), categories, rootMinLevel);
    }

    private static void generateDefaultLoggerNode(ClassOutput output) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className(LOGGER_NODE_CLASS_NAME)
                .classOutput(output).build()) {

            AnnotationCreator targetClass = cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass");
            targetClass.addValue("className", "org.jboss.logmanager.LoggerNode");

            final MethodCreator isLoggableLevelMethod = cc.getMethodCreator("isLoggableLevel", boolean.class, int.class);
            isLoggableLevelMethod.addAnnotation("com.oracle.svm.core.annotate.Alias");
            isLoggableLevelMethod.returnValue(isLoggableLevelMethod.load(false));
        }
    }

    private static void generateLogManagerLogger(ClassOutput output,
            BiFunction<MethodCreator, FieldDescriptor, BranchResult> isMinLevelEnabledFunction) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className(LOGMANAGER_LOGGER_CLASS_NAME)
                .classOutput(output).build()) {

            AnnotationCreator targetClass = cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass");
            targetClass.addValue("className", "org.jboss.logmanager.Logger");

            FieldCreator nameAlias = cc.getFieldCreator("name", String.class);
            nameAlias.addAnnotation("com.oracle.svm.core.annotate.Alias");

            FieldCreator loggerNodeAlias = cc.getFieldCreator("loggerNode", LOGGER_NODE_CLASS_NAME);
            loggerNodeAlias.addAnnotation("com.oracle.svm.core.annotate.Alias");

            final MethodCreator isLoggableMethod = cc.getMethodCreator("isLoggable", boolean.class,
                    java.util.logging.Level.class);
            isLoggableMethod.addAnnotation("com.oracle.svm.core.annotate.Substitute");

            final ResultHandle levelIntValue = getParamLevelIntValue(isLoggableMethod);

            final BranchResult levelBranch = isMinLevelEnabledFunction.apply(isLoggableMethod, nameAlias.getFieldDescriptor());

            final BytecodeCreator levelTrue = levelBranch.trueBranch();
            levelTrue.returnValue(
                    levelTrue.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(LOGGER_NODE_CLASS_NAME, "isLoggableLevel", boolean.class, int.class),
                            levelTrue.readInstanceField(loggerNodeAlias.getFieldDescriptor(), levelTrue.getThis()),
                            levelIntValue));

            final BytecodeCreator levelFalse = levelBranch.falseBranch();
            levelFalse.returnValue(levelFalse.load(false));
        }
    }

    private static ResultHandle getParamLevelIntValue(MethodCreator method) {
        final ResultHandle level = method.getMethodParam(0);
        return method
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Level.class, "intValue", int.class), level);
    }

    private static BiFunction<MethodCreator, FieldDescriptor, BranchResult> generateMinLevelDefault(
            String defaultMinLevelName) {
        return (method, nameAliasDescriptor) -> {
            final ResultHandle levelIntValue = getParamLevelIntValue(method);
            final ResultHandle infoLevelIntValue = getLogManagerLevelIntValue(defaultMinLevelName, method);
            return method.ifIntegerGreaterEqual(levelIntValue, infoLevelIntValue);
        };
    }

    private static ResultHandle getLogManagerLevelIntValue(String levelName, BytecodeCreator method) {
        final ResultHandle infoLevel = method.readStaticField(
                FieldDescriptor.of(org.jboss.logmanager.Level.class, levelName, org.jboss.logmanager.Level.class));
        return method
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Level.class, "intValue", int.class), infoLevel);
    }

    private static void generateDefaultLoggingLogger(Level minLevel, ClassOutput output) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className(LOGGING_LOGGER_CLASS_NAME)
                .classOutput(output).build()) {

            AnnotationCreator targetClass = cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass");
            targetClass.addValue("className", "org.jboss.logging.Logger");

            if (minLevel.intValue() >= org.jboss.logmanager.Level.INFO.intValue()) {
                // Constant fold these methods to return false,
                // since the build time log level is above this level.
                generateFalseFoldMethod("isTraceEnabled", cc);
                generateFalseFoldMethod("isDebugEnabled", cc);
            } else if (minLevel.intValue() == org.jboss.logmanager.Level.DEBUG.intValue()) {
                generateFalseFoldMethod("isTraceEnabled", cc);
            }
        }
    }

    /**
     * Generates a method that is constant-folded to always return false.
     */
    private static void generateFalseFoldMethod(String name, ClassCreator cc) {
        MethodCreator method = cc.getMethodCreator(name, boolean.class);
        method.addAnnotation("com.oracle.svm.core.annotate.Substitute");
        method.addAnnotation("org.graalvm.compiler.api.replacements.Fold");
        method.returnValue(method.load(false));
    }
}
