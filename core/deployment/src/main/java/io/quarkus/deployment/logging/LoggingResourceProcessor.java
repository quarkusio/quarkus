package io.quarkus.deployment.logging;

import static io.quarkus.runtime.logging.LoggingSetupRecorder.initializeBuildTimeLogging;

import java.lang.annotation.RetentionPolicy;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContextInitializer;
import org.jboss.logmanager.LogManager;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.LogCategoryMinLevelDefaultsBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogFileFormatBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.LogSocketFormatBuildItem;
import io.quarkus.deployment.builditem.LogSyslogFormatBuildItem;
import io.quarkus.deployment.builditem.NamedLogHandlersBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.StreamingLogHandlerBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.QuarkusCommand;
import io.quarkus.deployment.console.SetCompleter;
import io.quarkus.deployment.dev.ExceptionNotificationBuildItem;
import io.quarkus.deployment.dev.testing.MessageFormat;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.dev.console.CurrentAppExceptionHighlighter;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.This;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.logging.LoggingFilter;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.DecorateStackUtil;
import io.quarkus.runtime.logging.DiscoveredLogComponents;
import io.quarkus.runtime.logging.InheritableLevel;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.logging.LogBuildTimeConfig.CategoryBuildTimeConfig;
import io.quarkus.runtime.logging.LogCleanupFilterElement;
import io.quarkus.runtime.logging.LogFilterFactory;
import io.quarkus.runtime.logging.LogMetricsHandlerRecorder;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfig;

public final class LoggingResourceProcessor {

    private static final String LOGMANAGER_LOGGER_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logmanager_Logger";
    private static final String LOGGING_LOGGER_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logging_Logger";
    private static final String LOGGER_NODE_CLASS_NAME = "io.quarkus.runtime.generated.Target_org_jboss_logmanager_LoggerNode";

    private static final String MIN_LEVEL_COMPUTE_CLASS_NAME = "io.quarkus.runtime.generated.MinLevelCompute";
    private static final MethodDesc IS_MIN_LEVEL_ENABLED = ClassMethodDesc.of(ClassDesc.of(MIN_LEVEL_COMPUTE_CLASS_NAME),
            "isMinLevelEnabled",
            boolean.class, int.class, String.class);

    public static final DotName LOGGING_FILTER = DotName.createSimple(LoggingFilter.class.getName());
    private static final DotName FILTER = DotName.createSimple(Filter.class.getName());
    private static final String ILLEGAL_LOGGING_FILTER_USE_MESSAGE = "'@" + LoggingFilter.class.getName()
            + "' can only be used on classes that implement '"
            + Filter.class.getName() + "' and that are marked as final.";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.jboss.threads", "JBoss Threads version"));
    }

    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    // ensures that InitialConfigurator uses the build time configured minimum log level
    @BuildStep
    void setMinLevelForInitialConfigurator(LogBuildTimeConfig logBuildTimeConfig,
            BuildProducer<SystemPropertyBuildItem> systemPropertyBuildItemBuildProducer,
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageSystemPropertyBuildItemBuildProducer) {
        Level effectiveMinLevel = logBuildTimeConfig.minLevel();
        // go through the category config and if there exists a min-level lower than the root min-level, use it
        for (CategoryBuildTimeConfig categoryBuildTimeConfig : logBuildTimeConfig.categories().values()) {
            InheritableLevel inheritableLevel = categoryBuildTimeConfig.minLevel();
            if (inheritableLevel.isInherited()) {
                continue;
            }
            Level categoryMinLevel = inheritableLevel.getLevel();
            if (categoryMinLevel.intValue() < effectiveMinLevel.intValue()) {
                effectiveMinLevel = categoryMinLevel;
            }
        }
        String key = "logging.initial-configurator.min-level";
        String value = "" + effectiveMinLevel.intValue();
        systemPropertyBuildItemBuildProducer.produce(new SystemPropertyBuildItem(key,
                value));
        nativeImageSystemPropertyBuildItemBuildProducer.produce(new NativeImageSystemPropertyBuildItem(key, value));
    }

    @BuildStep
    void setUpDefaultLevels(List<LogCategoryBuildItem> categories,
            Consumer<RunTimeConfigurationDefaultBuildItem> configOutput,
            Consumer<LogCategoryMinLevelDefaultsBuildItem> minLevelDefaultsOutput) {
        Map<String, InheritableLevel> minLevelDefaults = new HashMap<>();
        for (LogCategoryBuildItem category : categories) {
            configOutput.accept(
                    new RunTimeConfigurationDefaultBuildItem(
                            "quarkus.log.category.\"" + category.getCategory() + "\".level",
                            category.getLevel().toString()));
            if (category.isSetMinLevelDefault()) {
                minLevelDefaults.put(category.getCategory(), InheritableLevel.of(category.getLevel()));
            }
        }
        minLevelDefaultsOutput.accept(new LogCategoryMinLevelDefaultsBuildItem(minLevelDefaults));
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
        runtimeInit.accept(new RuntimeInitializedClassBuildItem(ConsoleHandler.class.getName()));
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("io.smallrye.common.ref.References$ReaperThread"));
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("io.smallrye.common.os.Process"));
        systemProp
                .accept(new NativeImageSystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager"));
        provider.accept(
                new ServiceProviderBuildItem(LogContextInitializer.class.getName(), InitialConfigurator.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LoggingSetupBuildItem setupLoggingRuntimeInit(
            final RecorderContext context,
            final LoggingSetupRecorder recorder,
            final CombinedIndexBuildItem combinedIndexBuildItem,
            final LogCategoryMinLevelDefaultsBuildItem categoryMinLevelDefaults,
            final Optional<StreamingLogHandlerBuildItem> streamingLogStreamHandlerBuildItem,
            final List<LogHandlerBuildItem> handlerBuildItems,
            final List<NamedLogHandlersBuildItem> namedHandlerBuildItems,
            final List<LogConsoleFormatBuildItem> consoleFormatItems,
            final List<LogFileFormatBuildItem> fileFormatItems,
            final List<LogSyslogFormatBuildItem> syslogFormatItems,
            final List<LogSocketFormatBuildItem> socketFormatItems,
            final Optional<ConsoleFormatterBannerBuildItem> possibleBannerBuildItem,
            final List<LogStreamBuildItem> logStreamBuildItems,
            final BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItemBuildProducer,
            final LaunchModeBuildItem launchModeBuildItem,
            final List<LogCleanupFilterBuildItem> logCleanupFilters,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            final BuildProducer<ServiceProviderBuildItem> serviceProviderBuildItemBuildProducer) {
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

            // New Dev UI Log Stream
            RuntimeValue<Optional<Handler>> streamingDevUiLogHandler = null;
            if (streamingLogStreamHandlerBuildItem.isPresent()) {
                streamingDevUiLogHandler = streamingLogStreamHandlerBuildItem.get().getHandlerValue();
            }

            boolean alwaysEnableLogStream = false;
            if (!logStreamBuildItems.isEmpty()) {
                alwaysEnableLogStream = true;
            }

            List<RuntimeValue<Optional<Formatter>>> possibleConsoleFormatters = consoleFormatItems.stream()
                    .map(LogConsoleFormatBuildItem::getFormatterValue).collect(Collectors.toList());
            List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters = fileFormatItems.stream()
                    .map(LogFileFormatBuildItem::getFormatterValue).collect(Collectors.toList());
            List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters = syslogFormatItems.stream()
                    .map(LogSyslogFormatBuildItem::getFormatterValue).collect(Collectors.toList());
            List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters = socketFormatItems.stream()
                    .map(LogSocketFormatBuildItem::getFormatterValue).collect(Collectors.toList());

            context.registerSubstitution(InheritableLevel.ActualLevel.class, String.class, InheritableLevel.Substitution.class);
            context.registerSubstitution(InheritableLevel.Inherited.class, String.class, InheritableLevel.Substitution.class);

            DiscoveredLogComponents discoveredLogComponents = discoverLogComponents(combinedIndexBuildItem.getIndex());
            if (!discoveredLogComponents.getNameToFilterClass().isEmpty()) {
                reflectiveClassBuildItemBuildProducer.produce(
                        ReflectiveClassBuildItem.builder(discoveredLogComponents.getNameToFilterClass().values().toArray(
                                EMPTY_STRING_ARRAY)).reason(getClass().getName()).build());
                serviceProviderBuildItemBuildProducer
                        .produce(ServiceProviderBuildItem.allProvidersFromClassPath(LogFilterFactory.class.getName()));
            }

            shutdownListenerBuildItemBuildProducer.produce(new ShutdownListenerBuildItem(
                    recorder.initializeLogging(discoveredLogComponents,
                            categoryMinLevelDefaults.content, alwaysEnableLogStream,
                            streamingDevUiLogHandler, handlers, namedHandlers,
                            possibleConsoleFormatters, possibleFileFormatters, possibleSyslogFormatters,
                            possibleSocketFormatters,
                            possibleSupplier, launchModeBuildItem.getLaunchMode(), true)));

            List<LogCleanupFilterElement> additionalLogCleanupFilters = new ArrayList<>(logCleanupFilters.size());
            for (LogCleanupFilterBuildItem i : logCleanupFilters) {
                LogCleanupFilterElement filterElement = i.getFilterElement();
                additionalLogCleanupFilters.add(new LogCleanupFilterElement(
                        filterElement.getLoggerName(),
                        filterElement.getTargetLevel() == null ? org.jboss.logmanager.Level.DEBUG
                                : filterElement.getTargetLevel(),
                        filterElement.getMessageStarts()));
            }

            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            LogBuildTimeConfig logBuildTimeConfig = config.getConfigMapping(LogBuildTimeConfig.class);
            LogRuntimeConfig logRuntimeConfigInBuild = config.getConfigMapping(LogRuntimeConfig.class);
            ConsoleRuntimeConfig consoleRuntimeConfig = config.getConfigMapping(ConsoleRuntimeConfig.class);

            initializeBuildTimeLogging(logRuntimeConfigInBuild, logBuildTimeConfig, consoleRuntimeConfig,
                    categoryMinLevelDefaults.content, additionalLogCleanupFilters, launchModeBuildItem.getLaunchMode());
            // Build time logging is terminated before the application is started, after dev services are started.
            // When there is no devservices build time logging is still closed at deployment classloader close #closeBuildTimeLogging
        }
        return new LoggingSetupBuildItem();
    }

    private DiscoveredLogComponents discoverLogComponents(IndexView index) {
        Collection<AnnotationInstance> loggingFilterInstances = index.getAnnotations(LOGGING_FILTER);
        DiscoveredLogComponents result = new DiscoveredLogComponents();

        Map<String, String> filtersMap = new HashMap<>();
        for (AnnotationInstance instance : loggingFilterInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                throw new IllegalStateException("Unimplemented mode of use of '" + LoggingFilter.class.getName() + "'");
            }
            ClassInfo classInfo = target.asClass();
            boolean isFilterImpl = false;
            ClassInfo currentClassInfo = classInfo;
            while ((currentClassInfo != null) && (!JandexUtil.DOTNAME_OBJECT.equals(currentClassInfo.name()))) {
                boolean hasFilterInterface = false;
                List<DotName> ifaces = currentClassInfo.interfaceNames();
                for (DotName iface : ifaces) {
                    if (FILTER.equals(iface)) {
                        hasFilterInterface = true;
                        break;
                    }
                }
                if (hasFilterInterface) {
                    isFilterImpl = true;
                    break;
                }
                currentClassInfo = index.getClassByName(currentClassInfo.superName());
            }
            if (!isFilterImpl) {
                throw new RuntimeException(
                        ILLEGAL_LOGGING_FILTER_USE_MESSAGE + " Offending class is '" + classInfo.name() + "'");
            }

            String filterName = instance.value("name").asString();
            if (filtersMap.containsKey(filterName)) {
                throw new RuntimeException("Filter '" + filterName + "' was defined multiple times.");
            }
            filtersMap.put(filterName, classInfo.name().toString());
        }
        if (!filtersMap.isEmpty()) {
            result.setNameToFilterClass(filtersMap);
        }

        return result;
    }

    @BuildStep(onlyIfNot = IsProduction.class)
    @Produce(TestSetupBuildItem.class)
    @Produce(LogConsoleFormatBuildItem.class)
    @Consume(ConsoleInstalledBuildItem.class)
    void setupStackTraceFormatter(ApplicationArchivesBuildItem item, EffectiveIdeBuildItem ideSupport,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            List<ExceptionNotificationBuildItem> exceptionNotificationBuildItems,
            CuratedApplicationShutdownBuildItem curatedApplicationShutdownBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            LaunchModeBuildItem launchMode,
            LogBuildTimeConfig logBuildTimeConfig,
            BuildProducer<LoggingDecorateBuildItem> loggingDecorateProducer) {
        List<IndexView> indexList = new ArrayList<>();
        for (ApplicationArchive i : item.getAllApplicationArchives()) {
            if (i.getResolvedPaths().isSinglePath() && Files.isDirectory(i.getResolvedPaths().getSinglePath())) {
                indexList.add(i.getIndex());
            }
        }
        Path srcMainJava = getSourceRoot(curateOutcomeBuildItem.getApplicationModel(),
                outputTargetBuildItem.getOutputDirectory());

        CompositeIndex index = CompositeIndex.create(indexList);

        loggingDecorateProducer.produce(new LoggingDecorateBuildItem(srcMainJava, index));

        //awesome/horrible hack
        //we know from the index which classes are part of the current application
        //we add ANSI codes for bold and underline to their names to display them more prominently
        CurrentAppExceptionHighlighter.THROWABLE_FORMATTER = new BiConsumer<LogRecord, Consumer<LogRecord>>() {
            @Override
            public void accept(LogRecord logRecord, Consumer<LogRecord> logRecordConsumer) {
                StackTraceElement lastUserCode = null;
                Map<Throwable, StackTraceElement[]> restore = new HashMap<>();
                Throwable c = logRecord.getThrown();
                while (c != null) {
                    StackTraceElement[] stackTrace = c.getStackTrace();
                    for (int i = 0; i < stackTrace.length; ++i) {
                        var elem = stackTrace[i];
                        if (index.getClassByName(DotName.createSimple(elem.getClassName())) != null) {
                            lastUserCode = stackTrace[i];

                            if (launchMode.getLaunchMode().equals(LaunchMode.DEVELOPMENT)
                                    && logBuildTimeConfig.decorateStacktraces()) {

                                String decoratedString = DecorateStackUtil.getDecoratedString(srcMainJava, elem);
                                if (decoratedString != null) {
                                    if (logRecord instanceof ExtLogRecord elr) {
                                        switch (elr.getFormatStyle()) {
                                            case MESSAGE_FORMAT -> {
                                                Object[] p = elr.getParameters(); // can be null
                                                Object[] np = p != null ? Arrays.copyOf(p, p.length + 1) : new Object[1];
                                                np[np.length - 1] = decoratedString;
                                                elr.setParameters(np);
                                                elr.setMessage(elr.getMessage() + "\n\n{" + (np.length - 1) + "}\n\n");
                                            }
                                            case PRINTF -> {
                                                Object[] p = elr.getParameters(); // can be null
                                                Object[] np = p != null ? Arrays.copyOf(p, p.length + 1) : new Object[1];
                                                np[np.length - 1] = decoratedString;
                                                elr.setParameters(np);
                                                elr.setMessage(elr.getMessage() + "\n\n%" + np.length + "$s",
                                                        ExtLogRecord.FormatStyle.PRINTF);
                                            }
                                            case NO_FORMAT -> {
                                                elr.setParameters(new Object[] {
                                                        elr.getMessage(),
                                                        decoratedString
                                                });
                                                elr.setMessage("{0}\n\n{1}\n\n");
                                            }
                                        }
                                    } else {
                                        Object[] p = logRecord.getParameters(); // can be null
                                        Object[] np = p != null ? Arrays.copyOf(p, p.length + 1) : new Object[1];
                                        np[np.length - 1] = decoratedString;
                                        logRecord.setParameters(np);
                                        logRecord.setMessage(logRecord.getMessage() + "\n\n{" + (np.length - 1) + "}\n\n");
                                    }
                                }
                            }

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
                if (logRecord.getThrown() != null) {
                    for (ExceptionNotificationBuildItem i : exceptionNotificationBuildItems) {
                        i.getExceptionHandler().accept(logRecord.getThrown(), lastUserCode);
                    }
                }
            }
        };
        curatedApplicationShutdownBuildItem.addCloseTask(() -> CurrentAppExceptionHighlighter.THROWABLE_FORMATTER = null, true);
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
        if (metricsCapability.isPresent() && log.metricsEnabled()) {
            recorder.initCounters();
            metrics.produce(new MetricsFactoryConsumerBuildItem(recorder.registerMetrics()));
            logHandler.produce(new LogHandlerBuildItem(recorder.getLogHandler()));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void setUpMinLevelLogging(LogBuildTimeConfig log,
            LogCategoryMinLevelDefaultsBuildItem categoryMinLevelDefaults,
            final BuildProducer<GeneratedClassBuildItem> gcProducer,
            final BuildProducer<GeneratedResourceBuildItem> grProducer) {
        ClassOutput output = new GeneratedClassGizmo2Adaptor(gcProducer, grProducer, false);
        generateDefaultLoggerNode(output);
        if (allRootMinLevelOrHigher(log.minLevel().intValue(), log.categories(), categoryMinLevelDefaults.content)) {
            Level minLevel = log.minLevel();
            generateDefaultLoggingLogger(minLevel, output);
            String defaultMinLevelName = minLevel.getName();
            generateLogManagerLogger(output, (b0, name, levelIntValue) -> {
                final Expr defaultLevelIntValue = getLogManagerLevelIntValue(defaultMinLevelName, b0);
                return b0.ge(levelIntValue, defaultLevelIntValue);
            });
        } else {
            Map<String, CategoryBuildTimeConfig> categories = log.categories();
            Level rootMinLevel = log.minLevel();
            generateMinLevelCompute(categories, categoryMinLevelDefaults.content, rootMinLevel, output);
            generateLogManagerLogger(output,
                    (b0, name, levelIntValue) -> b0.invokeStatic(IS_MIN_LEVEL_ENABLED, levelIntValue, name));
        }
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void closeBuildTimeLogging(List<DevServicesResultBuildItem> devServices) {
        if (devServices.isEmpty()) {
            ((QuarkusClassLoader) Thread.currentThread().getContextClassLoader()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    InitialConfigurator.DELAYED_HANDLER.buildTimeComplete();
                }
            });
        }
    }

    private static boolean allRootMinLevelOrHigher(
            int rootMinLogLevel,
            Map<String, CategoryBuildTimeConfig> categories,
            Map<String, InheritableLevel> categoryMinLevelDefaults) {
        Set<String> allConfiguredCategoryNames = new LinkedHashSet<>(categories.keySet());
        allConfiguredCategoryNames.addAll(categoryMinLevelDefaults.keySet());
        for (String categoryName : allConfiguredCategoryNames) {
            InheritableLevel categoryMinLevel = LoggingSetupRecorder.getLogLevelNoInheritance(categoryName, categories,
                    CategoryBuildTimeConfig::minLevel, categoryMinLevelDefaults);
            if (!categoryMinLevel.isInherited() && categoryMinLevel.getLevel().intValue() < rootMinLogLevel) {
                return false;
            }
        }
        return true;
    }

    private static void generateMinLevelCompute(Map<String, CategoryBuildTimeConfig> categories,
            Map<String, InheritableLevel> categoryMinLevelDefaults, Level rootMinLevel,
            ClassOutput output) {
        Gizmo g = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);
        g.class_(MIN_LEVEL_COMPUTE_CLASS_NAME, cc -> {
            cc.final_();
            cc.staticMethod("isMinLevelEnabled", mc -> {
                mc.returning(boolean.class);
                ParamVar level = mc.parameter("level", int.class);
                ParamVar name = mc.parameter("name", String.class);
                mc.body(b0 -> {
                    for (Map.Entry<String, CategoryBuildTimeConfig> entry : categories.entrySet()) {
                        final String category = entry.getKey();
                        final int categoryLevelIntValue = LoggingSetupRecorder
                                .getLogLevel(category, categories, CategoryBuildTimeConfig::minLevel,
                                        categoryMinLevelDefaults,
                                        rootMinLevel)
                                .intValue();
                        b0.if_(b0.objEquals(name, Const.of(category)), BlockCreator::returnTrue);
                        b0.if_(b0.invokeVirtual(
                                MethodDesc.of(String.class, "startsWith", boolean.class, String.class),
                                name, Const.of(category + ".")),
                                t1 -> t1.return_(t1.ge(level, categoryLevelIntValue)));
                    }
                    b0.return_(b0.ge(level, getLogManagerLevelIntValue(rootMinLevel.toString(), b0)));
                });
            });
        });
    }

    private static void generateDefaultLoggerNode(ClassOutput output) {
        Gizmo g = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);
        g.class_(LOGGER_NODE_CLASS_NAME, cc -> {
            cc.final_();
            cc.addAnnotation(TargetClass.class, ac -> ac.add(TargetClass::className, "org.jboss.logmanager.LoggerNode"));
            cc.method("isLoggableLevel", mc -> {
                mc.addAnnotation(Alias.class);
                mc.parameter("level", int.class);
                mc.returning(boolean.class);
                mc.body(BlockCreator::returnFalse);
            });
        });
    }

    interface MinLevelEnabledFunction {
        Expr apply(BlockCreator b0, Var name, Var levelInt);
    }

    private static void generateLogManagerLogger(ClassOutput output,
            MinLevelEnabledFunction isMinLevelEnabledFunction) {
        Gizmo gizmo = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);
        gizmo.class_(LOGMANAGER_LOGGER_CLASS_NAME, cc -> {
            cc.final_();
            This this_ = cc.this_();
            cc.addAnnotation(TargetClass.class, ac -> ac.add(TargetClass::value, org.jboss.logmanager.Logger.class));
            FieldDesc name = cc.field("name", fc -> {
                fc.setType(String.class);
                fc.addAnnotation(Alias.class);
            });
            FieldDesc loggerNode = cc.field("loggerNode", fc -> {
                fc.setType(ClassDesc.of(LOGGER_NODE_CLASS_NAME));
                fc.addAnnotation(Alias.class);
            });
            cc.method("isLoggable", mc -> {
                mc.returning(boolean.class);
                mc.addAnnotation(Substitute.class);
                ParamVar level = mc.parameter("level", Level.class);
                mc.body(b0 -> {
                    var levelInt = b0.localVar("levelInt",
                            b0.invokeVirtual(MethodDesc.of(Level.class, "intValue", int.class), level));
                    b0.ifNot(isMinLevelEnabledFunction.apply(b0, this_.field(name), levelInt), BlockCreator::returnFalse);
                    b0.return_(b0.invokeVirtual(
                            ClassMethodDesc.of(ClassDesc.of(LOGGER_NODE_CLASS_NAME), "isLoggableLevel", boolean.class,
                                    int.class),
                            this_.field(loggerNode), levelInt));
                });
            });
        });
    }

    private static Expr getLogManagerLevelIntValue(String levelName, BlockCreator b0) {
        FieldDesc fd = FieldDesc.of(switch (levelName) {
            case "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE" -> org.jboss.logmanager.Level.class;
            default -> Level.class;
        }, levelName);
        return b0.invokeVirtual(MethodDesc.of(Level.class, "intValue", int.class), b0.get(Expr.staticField(fd)));
    }

    private static void generateDefaultLoggingLogger(Level minLevel, ClassOutput output) {
        Gizmo gizmo = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);
        gizmo.class_(LOGGING_LOGGER_CLASS_NAME, cc -> {
            cc.final_();
            cc.addAnnotation(TargetClass.class, ac -> ac.add(TargetClass::className, "org.jboss.logging.Logger"));
            if (minLevel.intValue() >= org.jboss.logmanager.Level.INFO.intValue()) {
                // Constant fold these methods to return false,
                // since the build time log level is above this level.
                generateFalseFoldMethod("isTraceEnabled", cc);
                generateFalseFoldMethod("isDebugEnabled", cc);
            } else if (minLevel.intValue() == org.jboss.logmanager.Level.DEBUG.intValue()) {
                generateFalseFoldMethod("isTraceEnabled", cc);
            }
        });
    }

    /**
     * Generates a method that is constant-folded to always return false.
     */
    private static void generateFalseFoldMethod(String name, ClassCreator cc) {
        cc.method(name, mc -> {
            mc.public_();
            mc.returning(boolean.class);
            mc.addAnnotation(Substitute.class);
            mc.addAnnotation(ClassDesc.of("org.graalvm.compiler.api.replacements.Fold"), RetentionPolicy.RUNTIME, ac -> {
            });
            mc.body(BlockCreator::returnFalse);
        });
    }

    @BuildStep
    ConsoleCommandBuildItem logConsoleCommand() {
        return new ConsoleCommandBuildItem(new LogCommand());
    }

    private Path getSourceRoot(ApplicationModel applicationModel, Path target) {
        WorkspaceModule workspaceModule = applicationModel.getAppArtifact().getWorkspaceModule();
        if (workspaceModule != null) {
            return workspaceModule.getModuleDir().toPath().resolve(SRC_MAIN_JAVA);
        }

        if (target != null) {
            var baseDir = target.getParent();
            if (baseDir == null) {
                baseDir = target;
            }
            return baseDir.resolve(SRC_MAIN_JAVA);
        }
        return Paths.get(SRC_MAIN_JAVA);
    }

    private static final String SRC_MAIN_JAVA = "src/main/java";

    @GroupCommandDefinition(name = "log", description = "Logging Commands")
    public static class LogCommand implements GroupCommand {

        @Option(shortName = 'h', hasValue = false, overrideRequired = true)
        public boolean help;

        @Override
        public List<Command> getCommands() {
            return List.of(new SetLogLevelCommand());
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().writeln(commandInvocation.getHelpInfo());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "set-level", description = "Sets the log level for a logger")
    public static class SetLogLevelCommand extends QuarkusCommand {

        @Option(required = true, completer = LoggerCompleter.class, description = "The logger to modify")
        private String logger;

        @Option(required = true, completer = LevelCompleter.class, description = "The log level")
        private String level;

        @Override
        public CommandResult doExecute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            java.util.logging.Logger logger = LogManager.getLogManager().getLogger(this.logger);
            Level level = org.jboss.logmanager.Level.parse(this.level);
            logger.setLevel(level);
            //we also update the handler level
            //so that this works as expected
            for (Handler i : logger.getHandlers()) {
                if (i.getLevel().intValue() > level.intValue()) {
                    i.setLevel(level);
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    public static class LoggerCompleter implements OptionCompleter<CompleterInvocation> {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            String soFar = completerInvocation.getGivenCompleteValue();
            var loggers = LogManager.getLogManager().getLoggerNames();
            Set<String> possible = new HashSet<>();
            while (loggers.hasMoreElements()) {
                String name = loggers.nextElement();

                if (name.equals(soFar)) {
                    possible.add(name);
                } else if (name.startsWith(soFar)) {
                    //we just want to complete the next segment
                    int pos = name.indexOf('.', soFar.length() + 1);
                    if (pos == -1) {
                        possible.add(name);
                    } else {
                        possible.add(name.substring(0, pos) + ".");
                        completerInvocation.setAppendSpace(false);
                    }
                }
            }
            completerInvocation.setCompleterValues(possible);
        }
    }

    public static class LevelCompleter extends SetCompleter {

        @Override
        protected Set<String> allOptions(String soFar) {
            return Set.of(Logger.Level.TRACE.name(),
                    Logger.Level.DEBUG.name(),
                    Logger.Level.INFO.name(),
                    Logger.Level.WARN.name(),
                    Logger.Level.ERROR.name(),
                    Logger.Level.FATAL.name());
        }
    }
}
