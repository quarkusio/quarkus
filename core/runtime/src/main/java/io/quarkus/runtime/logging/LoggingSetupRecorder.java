package io.quarkus.runtime.logging;

import static io.smallrye.common.net.HostName.getQualifiedHostName;
import static io.smallrye.common.os.Process.getProcessName;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextInitializer;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.errormanager.OnlyOnceErrorManager;
import org.jboss.logmanager.filters.AllFilter;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.formatters.TextBannerFormatter;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;
import org.jboss.logmanager.handlers.SocketHandler;
import org.jboss.logmanager.handlers.SyslogHandler;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.dev.console.CurrentAppExceptionHighlighter;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.ExceptionReporting;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.LogBuildTimeConfig.CategoryBuildTimeConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.CategoryConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.CleanupFilterConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.ConsoleConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.FileConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.SocketConfig;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@Recorder
public class LoggingSetupRecorder {
    private static final org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(LoggingSetupRecorder.class);

    private final LogBuildTimeConfig logBuildTimeConfig;
    private final RuntimeValue<LogRuntimeConfig> logRuntimeConfig;
    private final RuntimeValue<ConsoleRuntimeConfig> consoleRuntimeConfig;

    public LoggingSetupRecorder(
            final LogBuildTimeConfig logBuildTimeConfig,
            final RuntimeValue<LogRuntimeConfig> logRuntimeConfig,
            final RuntimeValue<ConsoleRuntimeConfig> consoleRuntimeConfig) {
        this.logBuildTimeConfig = logBuildTimeConfig;
        this.logRuntimeConfig = logRuntimeConfig;
        this.consoleRuntimeConfig = consoleRuntimeConfig;
    }

    @SuppressWarnings("unused") //called via reflection, as it is in an isolated CL
    public static void handleFailedStart() {
        handleFailedStart(new RuntimeValue<>(Optional.empty()));
    }

    public static void handleFailedStart(RuntimeValue<Optional<Supplier<String>>> banner) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        // There may be cases where a Config with the mappings is already available, but we can't be sure, so we wrap
        // the original Config and map the logging classes.
        SmallRyeConfig loggingConfig = new SmallRyeConfigBuilder()
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .withMapping(LogBuildTimeConfig.class)
                .withMapping(LogRuntimeConfig.class)
                .withMapping(ConsoleRuntimeConfig.class)
                .withSources(new ConfigSource() {
                    @Override
                    public Set<String> getPropertyNames() {
                        Set<String> properties = new HashSet<>();
                        config.getPropertyNames().forEach(properties::add);
                        return properties;
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return config.getRawValue(propertyName);
                    }

                    @Override
                    public String getName() {
                        return "Logging Config";
                    }
                }).build();
        LogBuildTimeConfig logBuildTimeConfig = loggingConfig.getConfigMapping(LogBuildTimeConfig.class);
        LogRuntimeConfig logRuntimeConfig = loggingConfig.getConfigMapping(LogRuntimeConfig.class);
        ConsoleRuntimeConfig consoleRuntimeConfig = loggingConfig.getConfigMapping(ConsoleRuntimeConfig.class);
        new LoggingSetupRecorder(logBuildTimeConfig, new RuntimeValue<>(logRuntimeConfig),
                new RuntimeValue<>(consoleRuntimeConfig)).initializeLogging(
                        DiscoveredLogComponents.ofEmpty(), emptyMap(), false, null, emptyList(), emptyList(), emptyList(),
                        emptyList(),
                        emptyList(), emptyList(), banner, LaunchMode.DEVELOPMENT, false);
    }

    public ShutdownListener initializeLogging(
            final DiscoveredLogComponents discoveredLogComponents,
            final Map<String, InheritableLevel> categoryDefaultMinLevels,
            final boolean enableWebStream,
            final RuntimeValue<Optional<Handler>> streamingDevUiConsoleHandler,
            final List<RuntimeValue<Optional<Handler>>> additionalHandlers,
            final List<RuntimeValue<Map<String, Handler>>> additionalNamedHandlers,
            final List<RuntimeValue<Optional<Formatter>>> possibleConsoleFormatters,
            final List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier,
            final LaunchMode launchMode,
            final boolean includeFilters) {

        LogBuildTimeConfig buildConfig = logBuildTimeConfig;
        LogRuntimeConfig config = logRuntimeConfig.getValue();

        ShutdownNotifier shutdownNotifier = new ShutdownNotifier();
        Map<String, CategoryConfig> categories = config.categories();
        LogContext logContext = LogContext.getLogContext();
        Logger rootLogger = logContext.getLogger("");

        if (config.level().intValue() < buildConfig.minLevel().intValue()) {
            log.warnf(
                    "Root log level %s set below minimum logging level %s, promoting it to %s. Set the build time configuration property 'quarkus.log.min-level' to '%s' to avoid this warning",
                    config.level(), buildConfig.minLevel(), buildConfig.minLevel(), config.level());
            rootLogger.setLevel(buildConfig.minLevel());
        } else {
            rootLogger.setLevel(config.level());
        }

        ErrorManager errorManager = new OnlyOnceErrorManager();
        Map<String, CleanupFilterConfig> filters = config.filters();
        List<LogCleanupFilterElement> filterElements;
        if (filters.isEmpty()) {
            filterElements = emptyList();
        } else {
            filterElements = new ArrayList<>(filters.size());
            filters.forEach(new BiConsumer<>() {
                @Override
                public void accept(String loggerName, CleanupFilterConfig config) {
                    filterElements.add(new LogCleanupFilterElement(loggerName, config.targetLevel(), config.ifStartsWith()));
                }
            });
        }
        LogCleanupFilter cleanupFiler = new LogCleanupFilter(filterElements, shutdownNotifier);
        for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
            handler.setFilter(cleanupFiler);
        }

        Map<String, Filter> namedFilters = createNamedFilters(discoveredLogComponents);
        ArrayList<Handler> handlers = new ArrayList<>(
                3 + additionalHandlers.size() + (config.handlers().isPresent() ? config.handlers().get().size() : 0));

        if (config.console().enable()) {
            Handler consoleHandler = configureConsoleHandler(config.console(), consoleRuntimeConfig.getValue(), errorManager,
                    cleanupFiler,
                    namedFilters, possibleConsoleFormatters, possibleBannerSupplier, launchMode, includeFilters);
            errorManager = consoleHandler.getErrorManager();
            handlers.add(consoleHandler);
        }
        if (launchMode.isDevOrTest()) {
            handlers.add(new ExtHandler() {
                @Override
                protected void doPublish(ExtLogRecord record) {
                    if (record.getThrown() != null) {
                        ExceptionReporting.notifyException(record.getThrown());
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws SecurityException {
                }
            });
        }

        if (config.file().enable()) {
            handlers.add(configureFileHandler(config.file(), errorManager, cleanupFiler, namedFilters, possibleFileFormatters,
                    includeFilters));
        }

        if (config.syslog().enable()) {
            Handler syslogHandler = configureSyslogHandler(config.syslog(), errorManager, cleanupFiler, namedFilters,
                    possibleSyslogFormatters, includeFilters);
            if (syslogHandler != null) {
                handlers.add(syslogHandler);
            }
        }

        if (config.socket().enable()) {
            final Handler socketHandler = configureSocketHandler(config.socket(), errorManager, cleanupFiler,
                    namedFilters, possibleSocketFormatters, includeFilters);
            if (socketHandler != null) {
                handlers.add(socketHandler);
            }
        }

        if ((launchMode.isDevOrTest() || enableWebStream)
                && streamingDevUiConsoleHandler != null
                && streamingDevUiConsoleHandler.getValue().isPresent()) {

            Handler handler = streamingDevUiConsoleHandler.getValue().get();
            handler.setErrorManager(errorManager);
            handler.setFilter(new LogCleanupFilter(filterElements, shutdownNotifier));

            if (possibleBannerSupplier != null && possibleBannerSupplier.getValue().isPresent()) {
                Supplier<String> bannerSupplier = possibleBannerSupplier.getValue().get();
                String header = "\n" + bannerSupplier.get();
                handler.publish(new LogRecord(Level.INFO, header));
            }
            handlers.add(handler);
        }

        Map<String, Handler> namedHandlers = shouldCreateNamedHandlers(config, additionalNamedHandlers)
                ? createNamedHandlers(config, consoleRuntimeConfig.getValue(), additionalNamedHandlers,
                        possibleConsoleFormatters, possibleFileFormatters, possibleSyslogFormatters, possibleSocketFormatters,
                        errorManager, cleanupFiler, namedFilters, launchMode,
                        shutdownNotifier, includeFilters)
                : emptyMap();
        if (!categories.isEmpty()) {
            Map<String, Handler> additionalNamedHandlersMap;
            if (additionalNamedHandlers.isEmpty()) {
                additionalNamedHandlersMap = emptyMap();
            } else {
                additionalNamedHandlersMap = new HashMap<>();
                for (RuntimeValue<Map<String, Handler>> runtimeValue : additionalNamedHandlers) {
                    runtimeValue.getValue().forEach(
                            new AdditionalNamedHandlersConsumer(additionalNamedHandlersMap, errorManager, filterElements,
                                    shutdownNotifier));
                }
            }

            namedHandlers.putAll(additionalNamedHandlersMap);

            setUpCategoryLoggers(buildConfig, categoryDefaultMinLevels, categories, logContext, errorManager, namedHandlers,
                    true);
        }

        for (RuntimeValue<Optional<Handler>> additionalHandler : additionalHandlers) {
            final Optional<Handler> optional = additionalHandler.getValue();
            if (optional.isPresent()) {
                final Handler handler = optional.get();
                handler.setErrorManager(errorManager);
                handler.setFilter(cleanupFiler);
                handlers.add(handler);
            }
        }
        addNamedHandlersToRootHandlers(config.handlers(), namedHandlers, handlers, errorManager);
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setHandlers(handlers.toArray(LogContextInitializer.NO_HANDLERS));
        return shutdownNotifier;
    }

    private static Map<String, Filter> createNamedFilters(DiscoveredLogComponents discoveredLogComponents) {
        if (discoveredLogComponents.getNameToFilterClass().isEmpty()) {
            return emptyMap();
        }

        Map<String, Filter> nameToFilter = new HashMap<>();
        LogFilterFactory logFilterFactory = LogFilterFactory.load();
        discoveredLogComponents.getNameToFilterClass().forEach(new BiConsumer<>() {
            @Override
            public void accept(String name, String className) {
                try {
                    nameToFilter.put(name, logFilterFactory.create(className));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to create instance of Logging Filter '" + className + "'", e);
                }
            }
        });
        return nameToFilter;
    }

    /**
     * WARNING: this method is part of the recorder but is actually called statically at build time.
     * You may not push RuntimeValue's to it.
     */
    public static void initializeBuildTimeLogging(
            final LogRuntimeConfig config,
            final LogBuildTimeConfig buildConfig,
            final ConsoleRuntimeConfig consoleConfig,
            final Map<String, InheritableLevel> categoryDefaultMinLevels,
            final List<LogCleanupFilterElement> additionalLogCleanupFilters,
            final LaunchMode launchMode) {

        ShutdownNotifier dummy = new ShutdownNotifier();

        Map<String, CategoryConfig> categories = config.categories();
        LogContext logContext = LogContext.getLogContext();
        Logger rootLogger = logContext.getLogger("");

        rootLogger.setLevel(config.level());

        ErrorManager errorManager = new OnlyOnceErrorManager();
        Map<String, CleanupFilterConfig> filters = config.filters();
        List<LogCleanupFilterElement> filterElements = new ArrayList<>(filters.size() + additionalLogCleanupFilters.size());
        for (Entry<String, CleanupFilterConfig> entry : filters.entrySet()) {
            filterElements.add(new LogCleanupFilterElement(entry.getKey(), entry.getValue().targetLevel(),
                    entry.getValue().ifStartsWith()));
        }
        for (LogCleanupFilterElement logCleanupFilter : additionalLogCleanupFilters) {
            filterElements.add(new LogCleanupFilterElement(logCleanupFilter.getLoggerName(), logCleanupFilter.getTargetLevel(),
                    logCleanupFilter.getMessageStarts()));
        }
        LogCleanupFilter logCleanupFilter = new LogCleanupFilter(filterElements, dummy);

        ArrayList<Handler> handlers = new ArrayList<>(3);
        if (config.console().enable()) {
            Handler consoleHandler = configureConsoleHandler(config.console(), consoleConfig, errorManager, logCleanupFilter,
                    emptyMap(), emptyList(), new RuntimeValue<>(Optional.empty()), launchMode, false);
            errorManager = consoleHandler.getErrorManager();
            handlers.add(consoleHandler);
        }

        Map<String, Handler> namedHandlers = createNamedHandlers(config, consoleConfig, emptyList(),
                emptyList(), emptyList(), emptyList(), emptyList(), errorManager, logCleanupFilter,
                emptyMap(), launchMode, dummy, false);

        setUpCategoryLoggers(buildConfig, categoryDefaultMinLevels, categories, logContext, errorManager, namedHandlers, false);

        addNamedHandlersToRootHandlers(config.handlers(), namedHandlers, handlers, errorManager);
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setBuildTimeHandlers(handlers.toArray(LogContextInitializer.NO_HANDLERS));
    }

    private boolean shouldCreateNamedHandlers(
            LogRuntimeConfig logRuntimeConfig,
            List<RuntimeValue<Map<String, Handler>>> additionalNamedHandlers) {
        if (!logRuntimeConfig.categories().isEmpty()) {
            return true;
        }
        if (logRuntimeConfig.handlers().isPresent()) {
            return !logRuntimeConfig.handlers().get().isEmpty();
        }
        return !additionalNamedHandlers.isEmpty();
    }

    public static <T> Level getLogLevel(String categoryName, Map<String, T> categories,
            Function<T, InheritableLevel> levelExtractor, Map<String, InheritableLevel> categoryDefaults, Level rootMinLevel) {
        while (true) {
            InheritableLevel inheritableLevel = getLogLevelNoInheritance(categoryName, categories, levelExtractor,
                    categoryDefaults);
            if (!inheritableLevel.isInherited()) {
                return inheritableLevel.getLevel();
            }
            final int lastDotIndex = categoryName.lastIndexOf('.');
            if (lastDotIndex == -1) {
                return rootMinLevel;
            }
            categoryName = categoryName.substring(0, lastDotIndex);
        }
    }

    public static <T> InheritableLevel getLogLevelNoInheritance(String categoryName, Map<String, T> categories,
            Function<T, InheritableLevel> levelExtractor, Map<String, InheritableLevel> categoryDefaults) {
        T categoryConfig = categories.get(categoryName);
        InheritableLevel inheritableLevel = null;
        if (categoryConfig != null) {
            inheritableLevel = levelExtractor.apply(categoryConfig);
        }
        if (inheritableLevel == null) {
            inheritableLevel = categoryDefaults.get(categoryName);
        }
        if (inheritableLevel == null) {
            inheritableLevel = InheritableLevel.Inherited.INSTANCE;
        }
        return inheritableLevel;
    }

    private static Map<String, Handler> createNamedHandlers(
            LogRuntimeConfig config, ConsoleRuntimeConfig consoleRuntimeConfig,
            List<RuntimeValue<Map<String, Handler>>> additionalNamedHandlers,
            List<RuntimeValue<Optional<Formatter>>> possibleConsoleFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters,
            ErrorManager errorManager, LogCleanupFilter cleanupFilter,
            Map<String, Filter> namedFilters, LaunchMode launchMode,
            ShutdownNotifier shutdownHandler, boolean includeFilters) {
        Map<String, Handler> namedHandlers = new HashMap<>();
        for (Entry<String, ConsoleConfig> consoleConfigEntry : config.consoleHandlers().entrySet()) {
            ConsoleConfig namedConsoleConfig = consoleConfigEntry.getValue();
            if (!namedConsoleConfig.enable()) {
                continue;
            }
            final Handler consoleHandler = configureConsoleHandler(namedConsoleConfig, consoleRuntimeConfig,
                    errorManager, cleanupFilter, namedFilters, possibleConsoleFormatters, null, launchMode,
                    includeFilters);
            addToNamedHandlers(namedHandlers, consoleHandler, consoleConfigEntry.getKey());
        }
        for (Entry<String, FileConfig> fileConfigEntry : config.fileHandlers().entrySet()) {
            FileConfig namedFileConfig = fileConfigEntry.getValue();
            if (!namedFileConfig.enable()) {
                continue;
            }
            final Handler fileHandler = configureFileHandler(namedFileConfig, errorManager, cleanupFilter, namedFilters,
                    possibleFileFormatters, includeFilters);
            addToNamedHandlers(namedHandlers, fileHandler, fileConfigEntry.getKey());
        }
        for (Entry<String, LogRuntimeConfig.SyslogConfig> sysLogConfigEntry : config.syslogHandlers().entrySet()) {
            LogRuntimeConfig.SyslogConfig namedSyslogConfig = sysLogConfigEntry.getValue();
            if (!namedSyslogConfig.enable()) {
                continue;
            }
            final Handler syslogHandler = configureSyslogHandler(namedSyslogConfig, errorManager, cleanupFilter,
                    namedFilters, possibleSyslogFormatters, includeFilters);
            if (syslogHandler != null) {
                addToNamedHandlers(namedHandlers, syslogHandler, sysLogConfigEntry.getKey());
            }
        }
        for (Entry<String, SocketConfig> socketConfigEntry : config.socketHandlers().entrySet()) {
            SocketConfig namedSocketConfig = socketConfigEntry.getValue();
            if (!namedSocketConfig.enable()) {
                continue;
            }
            final Handler socketHandler = configureSocketHandler(namedSocketConfig, errorManager, cleanupFilter,
                    namedFilters, possibleSocketFormatters, includeFilters);
            if (socketHandler != null) {
                addToNamedHandlers(namedHandlers, socketHandler, socketConfigEntry.getKey());
            }
        }

        Map<String, Handler> additionalNamedHandlersMap;
        if (additionalNamedHandlers.isEmpty()) {
            additionalNamedHandlersMap = emptyMap();
        } else {
            additionalNamedHandlersMap = new HashMap<>();
            for (RuntimeValue<Map<String, Handler>> runtimeValue : additionalNamedHandlers) {
                runtimeValue.getValue().forEach(
                        new AdditionalNamedHandlersConsumer(additionalNamedHandlersMap, errorManager,
                                cleanupFilter.filterElements.values(), shutdownHandler));
            }
        }

        namedHandlers.putAll(additionalNamedHandlersMap);

        return namedHandlers;
    }

    private static void addToNamedHandlers(Map<String, Handler> namedHandlers, Handler handler, String handlerName) {
        if (namedHandlers.containsKey(handlerName)) {
            throw new RuntimeException(String.format("Only one handler can be configured with the same name '%s'",
                    handlerName));
        }
        namedHandlers.put(handlerName, handler);
        InitialConfigurator.DELAYED_HANDLER.addLoggingCloseTask(new Runnable() {
            @Override
            public void run() {
                handler.close();
            }
        });
    }

    private static void addNamedHandlersToCategory(
            CategoryConfig categoryConfig, Map<String, Handler> namedHandlers,
            Logger categoryLogger,
            ErrorManager errorManager,
            boolean checkHandlerLinks) {
        for (String categoryNamedHandler : categoryConfig.handlers().get()) {
            Handler handler = namedHandlers.get(categoryNamedHandler);
            if (handler != null) {
                categoryLogger.addHandler(handler);
                InitialConfigurator.DELAYED_HANDLER.addLoggingCloseTask(new Runnable() {
                    @Override
                    public void run() {
                        categoryLogger.removeHandler(handler);
                    }
                });
            } else if (checkHandlerLinks) {
                errorManager.error(String.format("Handler with name '%s' is linked to a category but not configured.",
                        categoryNamedHandler), null, ErrorManager.GENERIC_FAILURE);
            }
        }
    }

    private static void setUpCategoryLoggers(
            final LogBuildTimeConfig buildConfig,
            final Map<String, InheritableLevel> categoryDefaultMinLevels,
            final Map<String, CategoryConfig> categories,
            final LogContext logContext,
            final ErrorManager errorManager,
            final Map<String, Handler> namedHandlers,
            final boolean checkHandlerLinks) {

        for (Entry<String, CategoryConfig> entry : categories.entrySet()) {
            String categoryName = entry.getKey();
            CategoryConfig categoryConfig = entry.getValue();
            InheritableLevel categoryLevel = categoryConfig.level();

            Level logLevel = getLogLevel(categoryName, categories, CategoryConfig::level, emptyMap(), buildConfig.minLevel());
            Level minLogLevel = getLogLevel(categoryName, buildConfig.categories(), CategoryBuildTimeConfig::minLevel,
                    categoryDefaultMinLevels, buildConfig.minLevel());
            if (logLevel.intValue() < minLogLevel.intValue()) {
                String category = entry.getKey();
                log.warnf(
                        "Log level %s for category '%s' set below minimum logging level %s, promoting it to %s. " +
                                "Set the build time configuration property 'quarkus.log.category.\"%s\".min-level' to '%s' to avoid this warning",
                        logLevel, category, minLogLevel, minLogLevel, category, logLevel);

                categoryLevel = InheritableLevel.of(minLogLevel.toString());
            }

            Logger categoryLogger = logContext.getLogger(categoryName);
            if (!categoryLevel.isInherited()) {
                categoryLogger.setLevel(categoryLevel.getLevel());
            }
            categoryLogger.setUseParentHandlers(categoryConfig.useParentHandlers());
            if (categoryConfig.handlers().isPresent()) {
                addNamedHandlersToCategory(categoryConfig, namedHandlers, categoryLogger, errorManager, checkHandlerLinks);
            }
        }
    }

    private static void addNamedHandlersToRootHandlers(Optional<List<String>> handlerNames, Map<String, Handler> namedHandlers,
            ArrayList<Handler> effectiveHandlers, ErrorManager errorManager) {
        if (handlerNames.isEmpty()) {
            return;
        }
        if (handlerNames.get().isEmpty()) {
            return;
        }
        for (String namedHandler : handlerNames.get()) {
            Handler handler = namedHandlers.get(namedHandler);
            if (handler != null) {
                effectiveHandlers.add(handler);
            } else {
                errorManager.error(String.format("Handler with name '%s' is linked to a category but not configured.",
                        namedHandler), null, ErrorManager.GENERIC_FAILURE);
            }
        }
    }

    public void initializeLoggingForImageBuild() {
        if (ImageMode.current() == ImageMode.NATIVE_BUILD) {
            final ConsoleHandler handler = new ConsoleHandler(new PatternFormatter(
                    "%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n"));
            handler.setLevel(Level.INFO);
            InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
            InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] { handler });
        }
    }

    private static Handler configureConsoleHandler(
            final ConsoleConfig config,
            final ConsoleRuntimeConfig consoleRuntimeConfig,
            final ErrorManager defaultErrorManager,
            final LogCleanupFilter cleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier,
            LaunchMode launchMode,
            boolean includeFilters) {
        Formatter formatter = null;
        boolean formatterWarning = false;

        for (RuntimeValue<Optional<Formatter>> value : possibleFormatters) {
            if (formatter != null) {
                formatterWarning = true;
            }
            final Optional<Formatter> val = value.getValue();
            if (val.isPresent()) {
                formatter = val.get();
            }
        }
        boolean color = false;
        if (formatter == null) {
            Supplier<String> bannerSupplier = null;
            if (possibleBannerSupplier != null && possibleBannerSupplier.getValue().isPresent()) {
                bannerSupplier = possibleBannerSupplier.getValue().get();
            }
            if (isColorEnabled(consoleRuntimeConfig, config)) {
                formatter = new ColorPatternFormatter(config.darken(), config.format());
                color = true;
            } else {
                formatter = new PatternFormatter(config.format());
            }
            if (bannerSupplier != null) {
                formatter = new TextBannerFormatter(bannerSupplier, ExtFormatter.wrap(formatter, false));
            }
        }
        final ConsoleHandler consoleHandler = new ConsoleHandler(
                config.stderr() ? ConsoleHandler.Target.SYSTEM_ERR : ConsoleHandler.Target.SYSTEM_OUT, formatter);
        consoleHandler.setLevel(config.level());
        consoleHandler.setErrorManager(defaultErrorManager);
        applyFilter(includeFilters, defaultErrorManager, cleanupFilter, config.filter(), namedFilters, consoleHandler);

        Handler handler = config.async().legacyEnable().orElse(config.async().enable())
                ? createAsyncHandler(config.async(), config.level(), consoleHandler)
                : consoleHandler;

        if (color && launchMode.isDevOrTest() && !config.async().enable()) {
            final Handler delegate = handler;
            handler = new ExtHandler() {
                @Override
                protected void doPublish(ExtLogRecord record) {
                    BiConsumer<LogRecord, Consumer<LogRecord>> formatter = CurrentAppExceptionHighlighter.THROWABLE_FORMATTER;
                    if (formatter != null) {
                        formatter.accept(record, delegate::publish);
                    } else {
                        delegate.publish(record);
                    }
                }

                @Override
                public void flush() {
                    delegate.flush();
                }

                @Override
                public void close() throws SecurityException {
                    delegate.close();
                }
            };
        }

        if (formatterWarning) {
            handler.getErrorManager().error("Multiple console formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }

        return handler;
    }

    private static Handler configureFileHandler(final FileConfig config, final ErrorManager errorManager,
            final LogCleanupFilter cleanupFilter, Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters,
            final boolean includeFilters) {
        FileHandler handler;
        FileConfig.RotationConfig rotationConfig = config.rotation();
        if (!rotationConfig.enabled()) {
            handler = new FileHandler();
        } else if (rotationConfig.fileSuffix().isPresent()) {
            PeriodicSizeRotatingFileHandler periodicSizeRotatingFileHandler = new PeriodicSizeRotatingFileHandler();
            periodicSizeRotatingFileHandler.setSuffix(rotationConfig.fileSuffix().get());
            periodicSizeRotatingFileHandler.setRotateSize(rotationConfig.maxFileSize().asLongValue());
            periodicSizeRotatingFileHandler.setRotateOnBoot(rotationConfig.rotateOnBoot());
            periodicSizeRotatingFileHandler.setMaxBackupIndex(rotationConfig.maxBackupIndex());
            handler = periodicSizeRotatingFileHandler;
        } else {
            SizeRotatingFileHandler sizeRotatingFileHandler = new SizeRotatingFileHandler(
                    rotationConfig.maxFileSize().asLongValue(), rotationConfig.maxBackupIndex());
            sizeRotatingFileHandler.setRotateOnBoot(rotationConfig.rotateOnBoot());
            handler = sizeRotatingFileHandler;
        }

        Formatter formatter = null;
        boolean formatterWarning = false;
        for (RuntimeValue<Optional<Formatter>> value : possibleFileFormatters) {
            if (formatter != null) {
                formatterWarning = true;
            }
            final Optional<Formatter> val = value.getValue();
            if (val.isPresent()) {
                formatter = val.get();
            }
        }
        if (formatter == null) {
            formatter = new PatternFormatter(config.format());
        }
        handler.setFormatter(formatter);

        handler.setAppend(true);
        try {
            handler.setFile(config.path());
        } catch (FileNotFoundException e) {
            errorManager.error("Failed to set log file", e, ErrorManager.OPEN_FAILURE);
        }
        handler.setErrorManager(errorManager);
        handler.setLevel(config.level());
        handler.setFilter(cleanupFilter);
        if (config.encoding().isPresent()) {
            try {
                handler.setEncoding(config.encoding().get().name());
            } catch (UnsupportedEncodingException e) {
                errorManager.error("Failed to set character encoding", e, ErrorManager.GENERIC_FAILURE);
            }
        }
        applyFilter(includeFilters, errorManager, cleanupFilter, config.filter(), namedFilters, handler);

        if (formatterWarning) {
            handler.getErrorManager().error("Multiple file formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }

        if (config.async().legacyEnable().orElse(config.async().enable())) {
            return createAsyncHandler(config.async(), config.level(), handler);
        }
        return handler;
    }

    private static void applyFilter(boolean includeFilters, ErrorManager errorManager, LogCleanupFilter cleanupFilter,
            Optional<String> filterName, Map<String, Filter> namedFilters, Handler handler) {
        if (filterName.isEmpty() || !includeFilters) {
            handler.setFilter(cleanupFilter);
        } else {
            String name = filterName.get();
            Filter filter = namedFilters.get(name);
            if (filter == null) {
                errorManager.error("Unable to find named filter '" + name + "'", null, ErrorManager.GENERIC_FAILURE);
                handler.setFilter(cleanupFilter);
            } else {
                handler.setFilter(new AllFilter(List.of(cleanupFilter, filter)));
            }
        }
    }

    private static Handler configureSyslogHandler(final LogRuntimeConfig.SyslogConfig config, final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters,
            final boolean includeFilters) {
        try {
            final SyslogHandler handler = new SyslogHandler(config.endpoint().getHostString(), config.endpoint().getPort());
            handler.setAppName(config.appName().orElse(getProcessName()));
            handler.setHostname(config.hostname().orElse(getQualifiedHostName()));
            handler.setFacility(config.facility());
            handler.setSyslogType(config.syslogType());
            handler.setProtocol(config.protocol());
            handler.setBlockOnReconnect(config.blockOnReconnect());
            handler.setTruncate(config.truncate());
            handler.setUseCountingFraming(switch (config.useCountingFraming()) {
                case PROTOCOL_DEPENDENT ->
                    config.protocol() == SyslogHandler.Protocol.TCP || config.protocol() == SyslogHandler.Protocol.SSL_TCP;
                case TRUE -> true;
                case FALSE -> false;
            });
            handler.setLevel(config.level());
            if (config.maxLength().isPresent()) {
                BigInteger maxLen = config.maxLength().get().asBigInteger();
                if (maxLen.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                    errorManager.error(
                            "Using 2GB as the value of maxLength for SyslogHandler as it is the maximum allowed value", null,
                            ErrorManager.GENERIC_FAILURE);
                    maxLen = BigInteger.valueOf(Integer.MAX_VALUE);
                } else {
                    BigInteger minimumAllowedMaxLength = BigInteger.valueOf(128);
                    if (maxLen.compareTo(minimumAllowedMaxLength) < 0) {
                        errorManager.error(
                                "Using 128 as the value of maxLength for SyslogHandler as using a smaller value is not allowed",
                                null, ErrorManager.GENERIC_FAILURE);
                        maxLen = minimumAllowedMaxLength;
                    }
                }
                handler.setMaxLength(maxLen.intValue());
            }

            Formatter formatter = null;
            boolean formatterWarning = false;
            for (RuntimeValue<Optional<Formatter>> value : possibleSyslogFormatters) {
                if (formatter != null) {
                    formatterWarning = true;
                }
                final Optional<Formatter> val = value.getValue();
                if (val.isPresent()) {
                    formatter = val.get();
                }
            }
            if (formatter == null) {
                formatter = new PatternFormatter(config.format());
            }
            handler.setFormatter(formatter);

            handler.setErrorManager(errorManager);
            handler.setFilter(logCleanupFilter);
            applyFilter(includeFilters, errorManager, logCleanupFilter, config.filter(), namedFilters, handler);

            if (formatterWarning) {
                handler.getErrorManager().error("Multiple syslog formatters were activated", null,
                        ErrorManager.GENERIC_FAILURE);
            }

            if (config.async().legacyEnable().orElse(config.async().enable())) {
                return createAsyncHandler(config.async(), config.level(), handler);
            }
            return handler;
        } catch (IOException e) {
            errorManager.error("Failed to create syslog handler", e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }

    private static Handler configureSocketHandler(final LogRuntimeConfig.SocketConfig config,
            final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters,
            final boolean includeFilters) {
        try {
            final SocketHandler handler = new SocketHandler(config.endpoint().getHostString(), config.endpoint().getPort());
            handler.setProtocol(config.protocol());
            handler.setBlockOnReconnect(config.blockOnReconnect());
            handler.setLevel(config.level());

            Formatter formatter = null;
            boolean formatterWarning = false;
            for (RuntimeValue<Optional<Formatter>> value : possibleSocketFormatters) {
                if (formatter != null) {
                    formatterWarning = true;
                }
                final Optional<Formatter> val = value.getValue();
                if (val.isPresent()) {
                    formatter = val.get();
                }
            }
            if (formatter == null) {
                formatter = new PatternFormatter(config.format());
            }
            handler.setFormatter(formatter);

            handler.setErrorManager(errorManager);
            handler.setFilter(logCleanupFilter);
            applyFilter(includeFilters, errorManager, logCleanupFilter, config.filter(), namedFilters, handler);

            if (formatterWarning) {
                handler.getErrorManager().error("Multiple socket formatters were activated", null,
                        ErrorManager.GENERIC_FAILURE);
            }

            if (config.async().legacyEnable().orElse(config.async().enable())) {
                return createAsyncHandler(config.async(), config.level(), handler);
            }
            return handler;
        } catch (IOException e) {
            errorManager.error("Failed to create socket handler", e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }

    private static AsyncHandler createAsyncHandler(LogRuntimeConfig.AsyncConfig asyncConfig, Level level, Handler handler) {
        final AsyncHandler asyncHandler = new AsyncHandler(asyncConfig.queueLength());
        asyncHandler.setOverflowAction(asyncConfig.overflow());
        asyncHandler.addHandler(handler);
        asyncHandler.setLevel(level);
        return asyncHandler;
    }

    private static boolean isColorEnabled(ConsoleRuntimeConfig consoleConfig, ConsoleConfig logConfig) {
        if (consoleConfig.color().isPresent()) {
            return consoleConfig.color().get();
        }
        if (logConfig.color().isPresent()) {
            return logConfig.color().get();
        }
        return QuarkusConsole.hasColorSupport();
    }

    private static class AdditionalNamedHandlersConsumer implements BiConsumer<String, Handler> {
        private final Map<String, Handler> additionalNamedHandlersMap;
        private final ErrorManager errorManager;
        private final Collection<LogCleanupFilterElement> filterElements;

        private final ShutdownNotifier shutdownNotifier;

        public AdditionalNamedHandlersConsumer(Map<String, Handler> additionalNamedHandlersMap, ErrorManager errorManager,
                Collection<LogCleanupFilterElement> filterElements, ShutdownNotifier shutdownNotifier) {
            this.additionalNamedHandlersMap = additionalNamedHandlersMap;
            this.errorManager = errorManager;
            this.filterElements = filterElements;
            this.shutdownNotifier = shutdownNotifier;
        }

        @Override
        public void accept(String name, Handler handler) {
            Handler previous = additionalNamedHandlersMap.putIfAbsent(name, handler);
            if (previous != null) {
                throw new IllegalStateException(String.format(
                        "Duplicate key %s (attempted merging values %s and %s)",
                        name, previous, handler));
            }
            handler.setErrorManager(errorManager);
            handler.setFilter(new LogCleanupFilter(filterElements, shutdownNotifier));
        }
    }

    public static class ShutdownNotifier implements ShutdownListener {
        volatile boolean shutdown;

        @Override
        public void shutdown(ShutdownNotification notification) {
            shutdown = true;
            notification.done();
        }
    }
}
