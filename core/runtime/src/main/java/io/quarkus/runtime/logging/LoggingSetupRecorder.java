package io.quarkus.runtime.logging;

import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.errormanager.OnlyOnceErrorManager;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;
import org.jboss.logmanager.handlers.SyslogHandler;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.dev.console.CurrentAppExceptionHighlighter;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.util.ColorSupport;

/**
 *
 */
@Recorder
public class LoggingSetupRecorder {

    private static final org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(LoggingSetupRecorder.class);

    public LoggingSetupRecorder() {
    }

    @SuppressWarnings("unused") //called via reflection, as it is in an isolated CL
    public static void handleFailedStart() {
        handleFailedStart(new RuntimeValue<>(Optional.empty()));
    }

    public static void handleFailedStart(RuntimeValue<Optional<Supplier<String>>> banner) {
        LogConfig config = new LogConfig();
        ConfigInstantiator.handleObject(config);
        LogBuildTimeConfig buildConfig = new LogBuildTimeConfig();
        ConfigInstantiator.handleObject(buildConfig);
        ConsoleRuntimeConfig consoleRuntimeConfig = new ConsoleRuntimeConfig();
        ConfigInstantiator.handleObject(consoleRuntimeConfig);
        new LoggingSetupRecorder().initializeLogging(config, buildConfig, consoleRuntimeConfig, Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(), banner, LaunchMode.DEVELOPMENT);
    }

    public void initializeLogging(LogConfig config, LogBuildTimeConfig buildConfig,
            ConsoleRuntimeConfig consoleConfig,
            final List<RuntimeValue<Optional<Handler>>> additionalHandlers,
            final List<RuntimeValue<Map<String, Handler>>> additionalNamedHandlers,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier, LaunchMode launchMode) {

        final Map<String, CategoryConfig> categories = config.categories;
        final LogContext logContext = LogContext.getLogContext();
        final Logger rootLogger = logContext.getLogger("");

        rootLogger.setLevel(config.level);

        ErrorManager errorManager = new OnlyOnceErrorManager();
        final Map<String, CleanupFilterConfig> filters = config.filters;
        List<LogCleanupFilterElement> filterElements;
        if (filters.isEmpty()) {
            filterElements = Collections.emptyList();
        } else {
            filterElements = new ArrayList<>(filters.size());
            filters.forEach(new BiConsumer<String, CleanupFilterConfig>() {
                @Override
                public void accept(String loggerName, CleanupFilterConfig config) {
                    filterElements.add(
                            new LogCleanupFilterElement(loggerName, config.targetLevel, config.ifStartsWith));
                }
            });
        }

        final ArrayList<Handler> handlers = new ArrayList<>(3 + additionalHandlers.size());

        if (config.console.enable) {
            final Handler consoleHandler = configureConsoleHandler(config.console, consoleConfig, errorManager, filterElements,
                    possibleFormatters, possibleBannerSupplier, launchMode);
            errorManager = consoleHandler.getErrorManager();
            handlers.add(consoleHandler);
        }

        if (config.file.enable) {
            handlers.add(configureFileHandler(config.file, errorManager, filterElements));
        }

        if (config.syslog.enable) {
            final Handler syslogHandler = configureSyslogHandler(config.syslog, errorManager, filterElements);
            if (syslogHandler != null) {
                handlers.add(syslogHandler);
            }
        }

        if (!categories.isEmpty()) {
            Map<String, Handler> namedHandlers = createNamedHandlers(config, consoleConfig, possibleFormatters, errorManager,
                    filterElements, launchMode);

            Map<String, Handler> additionalNamedHandlersMap;
            if (additionalNamedHandlers.isEmpty()) {
                additionalNamedHandlersMap = Collections.emptyMap();
            } else {
                additionalNamedHandlersMap = new HashMap<>();
                for (RuntimeValue<Map<String, Handler>> runtimeValue : additionalNamedHandlers) {
                    runtimeValue.getValue().forEach(
                            new AdditionalNamedHandlersConsumer(additionalNamedHandlersMap, errorManager, filterElements));
                }
            }

            namedHandlers.putAll(additionalNamedHandlersMap);

            categories.forEach(new BiConsumer<String, CategoryConfig>() {
                @Override
                public void accept(String categoryName, CategoryConfig config) {
                    final CategoryBuildTimeConfig buildCategory = isSubsetOf(categoryName, buildConfig.categories);
                    final Level logLevel = getLogLevel(categoryName, config, categories, buildConfig.minLevel);
                    final Level minLogLevel = buildCategory == null
                            ? buildConfig.minLevel
                            : buildCategory.minLevel.getLevel();

                    if (logLevel.intValue() < minLogLevel.intValue()) {
                        log.warnf("Log level %s for category '%s' set below minimum logging level %s, promoting it to %s",
                                logLevel,
                                categoryName, minLogLevel, minLogLevel);

                        config.level = InheritableLevel.of(minLogLevel.toString());
                    }
                }
            });
            categories.forEach(new CategoryLoggerConsumer(logContext, namedHandlers, errorManager));
        }

        for (RuntimeValue<Optional<Handler>> additionalHandler : additionalHandlers) {
            final Optional<Handler> optional = additionalHandler.getValue();
            if (optional.isPresent()) {
                final Handler handler = optional.get();
                handler.setErrorManager(errorManager);
                handler.setFilter(new LogCleanupFilter(filterElements));
                handlers.add(handler);
            }
        }

        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setHandlers(handlers.toArray(EmbeddedConfigurator.NO_HANDLERS));
    }

    public static void initializeBuildTimeLogging(LogConfig config, LogBuildTimeConfig buildConfig,
            ConsoleRuntimeConfig consoleConfig, LaunchMode launchMode) {

        final Map<String, CategoryConfig> categories = config.categories;
        final LogContext logContext = LogContext.getLogContext();
        final Logger rootLogger = logContext.getLogger("");

        rootLogger.setLevel(config.level);

        ErrorManager errorManager = new OnlyOnceErrorManager();
        final Map<String, CleanupFilterConfig> filters = config.filters;
        List<LogCleanupFilterElement> filterElements = new ArrayList<>(filters.size());
        for (Entry<String, CleanupFilterConfig> entry : filters.entrySet()) {
            filterElements.add(
                    new LogCleanupFilterElement(entry.getKey(), entry.getValue().targetLevel, entry.getValue().ifStartsWith));
        }

        final ArrayList<Handler> handlers = new ArrayList<>(3);

        if (config.console.enable) {
            final Handler consoleHandler = configureConsoleHandler(config.console, consoleConfig, errorManager, filterElements,
                    Collections.emptyList(), new RuntimeValue<>(Optional.empty()), launchMode);
            errorManager = consoleHandler.getErrorManager();
            handlers.add(consoleHandler);
        }

        Map<String, Handler> namedHandlers = createNamedHandlers(config, consoleConfig, Collections.emptyList(), errorManager,
                filterElements, launchMode);

        for (Map.Entry<String, CategoryConfig> entry : categories.entrySet()) {
            final CategoryBuildTimeConfig buildCategory = isSubsetOf(entry.getKey(), buildConfig.categories);
            final Level logLevel = getLogLevel(entry.getKey(), entry.getValue(), categories, buildConfig.minLevel);
            final Level minLogLevel = buildCategory == null
                    ? buildConfig.minLevel
                    : buildCategory.minLevel.getLevel();

            if (logLevel.intValue() < minLogLevel.intValue()) {
                log.warnf("Log level %s for category '%s' set below minimum logging level %s, promoting it to %s", logLevel,
                        entry.getKey(), minLogLevel, minLogLevel);

                entry.getValue().level = InheritableLevel.of(minLogLevel.toString());
            }
        }

        for (Map.Entry<String, CategoryConfig> entry : categories.entrySet()) {
            final String name = entry.getKey();
            final Logger categoryLogger = logContext.getLogger(name);
            final CategoryConfig categoryConfig = entry.getValue();
            if (!categoryConfig.level.isInherited()) {
                categoryLogger.setLevel(categoryConfig.level.getLevel());
            }
            categoryLogger.setUseParentHandlers(categoryConfig.useParentHandlers);
            if (categoryConfig.handlers.isPresent()) {
                addNamedHandlersToCategory(categoryConfig, namedHandlers, categoryLogger, errorManager);
            }
        }
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setBuildTimeHandlers(handlers.toArray(EmbeddedConfigurator.NO_HANDLERS));
    }

    private static Level getLogLevel(String categoryName, CategoryConfig categoryConfig, Map<String, CategoryConfig> categories,
            Level rootMinLevel) {
        if (Objects.isNull(categoryConfig))
            return rootMinLevel;

        final InheritableLevel inheritableLevel = categoryConfig.level;
        if (!inheritableLevel.isInherited())
            return inheritableLevel.getLevel();

        int lastDotIndex = categoryName.lastIndexOf('.');
        if (lastDotIndex == -1)
            return rootMinLevel;

        String parent = categoryName.substring(0, lastDotIndex);
        return getLogLevel(parent, categories.get(parent), categories, rootMinLevel);
    }

    private static CategoryBuildTimeConfig isSubsetOf(String categoryName, Map<String, CategoryBuildTimeConfig> categories) {
        return categories.entrySet().stream()
                .filter(buildCategoryEntry -> categoryName.startsWith(buildCategoryEntry.getKey()))
                .map(Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Handler> createNamedHandlers(LogConfig config, ConsoleRuntimeConfig consoleRuntimeConfig,
            List<RuntimeValue<Optional<Formatter>>> possibleFormatters, ErrorManager errorManager,
            List<LogCleanupFilterElement> filterElements, LaunchMode launchMode) {
        Map<String, Handler> namedHandlers = new HashMap<>();
        for (Entry<String, ConsoleConfig> consoleConfigEntry : config.consoleHandlers.entrySet()) {
            ConsoleConfig namedConsoleConfig = consoleConfigEntry.getValue();
            if (!namedConsoleConfig.enable) {
                continue;
            }
            final Handler consoleHandler = configureConsoleHandler(namedConsoleConfig, consoleRuntimeConfig, errorManager,
                    filterElements,
                    possibleFormatters, null, launchMode);
            addToNamedHandlers(namedHandlers, consoleHandler, consoleConfigEntry.getKey());
        }
        for (Entry<String, FileConfig> fileConfigEntry : config.fileHandlers.entrySet()) {
            FileConfig namedFileConfig = fileConfigEntry.getValue();
            if (!namedFileConfig.enable) {
                continue;
            }
            final Handler fileHandler = configureFileHandler(namedFileConfig, errorManager, filterElements);
            addToNamedHandlers(namedHandlers, fileHandler, fileConfigEntry.getKey());
        }
        for (Entry<String, SyslogConfig> sysLogConfigEntry : config.syslogHandlers.entrySet()) {
            SyslogConfig namedSyslogConfig = sysLogConfigEntry.getValue();
            if (!namedSyslogConfig.enable) {
                continue;
            }
            final Handler syslogHandler = configureSyslogHandler(namedSyslogConfig, errorManager, filterElements);
            if (syslogHandler != null) {
                addToNamedHandlers(namedHandlers, syslogHandler, sysLogConfigEntry.getKey());
            }
        }
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

    private static void addNamedHandlersToCategory(CategoryConfig categoryConfig, Map<String, Handler> namedHandlers,
            Logger categoryLogger,
            ErrorManager errorManager) {
        for (String categoryNamedHandler : categoryConfig.handlers.get()) {
            Handler handler = namedHandlers.get(categoryNamedHandler);
            if (handler != null) {
                categoryLogger.addHandler(handler);
                InitialConfigurator.DELAYED_HANDLER.addLoggingCloseTask(new Runnable() {
                    @Override
                    public void run() {
                        categoryLogger.removeHandler(handler);
                    }
                });
            } else {
                errorManager.error(String.format("Handler with name '%s' is linked to a category but not configured.",
                        categoryNamedHandler), null, ErrorManager.GENERIC_FAILURE);
            }
        }
    }

    public void initializeLoggingForImageBuild() {
        if (ImageInfo.inImageBuildtimeCode()) {
            final ConsoleHandler handler = new ConsoleHandler(new PatternFormatter(
                    "%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n"));
            handler.setLevel(Level.INFO);
            InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
            InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] { handler });
        }
    }

    private static Handler configureConsoleHandler(final ConsoleConfig config, ConsoleRuntimeConfig consoleRuntimeConfig,
            final ErrorManager defaultErrorManager,
            final List<LogCleanupFilterElement> filterElements,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier, LaunchMode launchMode) {
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
            if (ColorSupport.isColorEnabled(consoleRuntimeConfig, config)) {
                color = true;
                ColorPatternFormatter colorPatternFormatter = new ColorPatternFormatter(config.darken,
                        config.format);
                if (bannerSupplier != null) {
                    formatter = new BannerFormatter(colorPatternFormatter, true, bannerSupplier);
                } else {
                    formatter = colorPatternFormatter;
                }
            } else {
                PatternFormatter patternFormatter = new PatternFormatter(config.format);
                if (bannerSupplier != null) {
                    formatter = new BannerFormatter(patternFormatter, false, bannerSupplier);
                } else {
                    formatter = patternFormatter;
                }
            }
        }
        final ConsoleHandler consoleHandler = new ConsoleHandler(
                config.stderr ? ConsoleHandler.Target.SYSTEM_ERR : ConsoleHandler.Target.SYSTEM_OUT, formatter);
        consoleHandler.setLevel(config.level);
        consoleHandler.setErrorManager(defaultErrorManager);
        consoleHandler.setFilter(new LogCleanupFilter(filterElements));

        Handler handler = config.async.enable ? createAsyncHandler(config.async, config.level, consoleHandler)
                : consoleHandler;
        if (color && launchMode.isDevOrTest() && !config.async.enable) {
            final Handler delegate = handler;
            handler = new Handler() {
                @Override
                public void publish(LogRecord record) {
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
            handler.getErrorManager().error("Multiple formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }

        return handler;
    }

    private static Handler configureFileHandler(final FileConfig config, final ErrorManager errorManager,
            final List<LogCleanupFilterElement> filterElements) {
        FileHandler handler = new FileHandler();
        FileConfig.RotationConfig rotationConfig = config.rotation;
        if ((rotationConfig.maxFileSize.isPresent() || rotationConfig.rotateOnBoot)
                && rotationConfig.fileSuffix.isPresent()) {
            PeriodicSizeRotatingFileHandler periodicSizeRotatingFileHandler = new PeriodicSizeRotatingFileHandler();
            periodicSizeRotatingFileHandler.setSuffix(rotationConfig.fileSuffix.get());
            rotationConfig.maxFileSize
                    .ifPresent(memorySize -> periodicSizeRotatingFileHandler.setRotateSize(memorySize.asLongValue()));
            periodicSizeRotatingFileHandler.setRotateOnBoot(rotationConfig.rotateOnBoot);
            periodicSizeRotatingFileHandler.setMaxBackupIndex(rotationConfig.maxBackupIndex);
            handler = periodicSizeRotatingFileHandler;
        } else if (rotationConfig.maxFileSize.isPresent()) {
            SizeRotatingFileHandler sizeRotatingFileHandler = new SizeRotatingFileHandler(
                    rotationConfig.maxFileSize.get().asLongValue(), rotationConfig.maxBackupIndex);
            sizeRotatingFileHandler.setRotateOnBoot(rotationConfig.rotateOnBoot);
            handler = sizeRotatingFileHandler;
        } else if (rotationConfig.fileSuffix.isPresent()) {
            PeriodicRotatingFileHandler periodicRotatingFileHandler = new PeriodicRotatingFileHandler();
            periodicRotatingFileHandler.setSuffix(rotationConfig.fileSuffix.get());
            handler = periodicRotatingFileHandler;
        }

        final PatternFormatter formatter = new PatternFormatter(config.format);
        handler.setFormatter(formatter);
        handler.setAppend(true);
        try {
            handler.setFile(config.path);
        } catch (FileNotFoundException e) {
            errorManager.error("Failed to set log file", e, ErrorManager.OPEN_FAILURE);
        }
        handler.setErrorManager(errorManager);
        handler.setLevel(config.level);
        handler.setFilter(new LogCleanupFilter(filterElements));
        if (config.async.enable) {
            return createAsyncHandler(config.async, config.level, handler);
        }
        return handler;
    }

    private static Handler configureSyslogHandler(final SyslogConfig config,
            final ErrorManager errorManager,
            final List<LogCleanupFilterElement> filterElements) {
        try {
            final SyslogHandler handler = new SyslogHandler(config.endpoint.getHostString(), config.endpoint.getPort());
            handler.setAppName(config.appName.orElse(getProcessName()));
            handler.setHostname(config.hostname.orElse(getQualifiedHostName()));
            handler.setFacility(config.facility);
            handler.setSyslogType(config.syslogType);
            handler.setProtocol(config.protocol);
            handler.setBlockOnReconnect(config.blockOnReconnect);
            handler.setTruncate(config.truncate);
            handler.setUseCountingFraming(config.useCountingFraming);
            handler.setLevel(config.level);
            final PatternFormatter formatter = new PatternFormatter(config.format);
            handler.setFormatter(formatter);
            handler.setErrorManager(errorManager);
            handler.setFilter(new LogCleanupFilter(filterElements));
            if (config.async.enable) {
                return createAsyncHandler(config.async, config.level, handler);
            }
            return handler;
        } catch (IOException e) {
            errorManager.error("Failed to create syslog handler", e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }

    private static AsyncHandler createAsyncHandler(AsyncConfig asyncConfig, Level level, Handler handler) {
        final AsyncHandler asyncHandler = new AsyncHandler(asyncConfig.queueLength);
        asyncHandler.setOverflowAction(asyncConfig.overflow);
        asyncHandler.addHandler(handler);
        asyncHandler.setLevel(level);
        return asyncHandler;
    }

    private static class CategoryLoggerConsumer implements BiConsumer<String, CategoryConfig> {
        private final LogContext logContext;
        private final Map<String, Handler> namedHandlers;
        private final ErrorManager errorManager;

        CategoryLoggerConsumer(LogContext logContext, Map<String, Handler> namedHandlers, ErrorManager errorManager) {
            this.logContext = logContext;
            this.namedHandlers = namedHandlers;
            this.errorManager = errorManager;
        }

        @Override
        public void accept(String name, CategoryConfig categoryConfig) {
            final Logger categoryLogger = logContext.getLogger(name);
            if (!categoryConfig.level.isInherited()) {
                categoryLogger.setLevel(categoryConfig.level.getLevel());
            }
            categoryLogger.setUseParentHandlers(categoryConfig.useParentHandlers);
            if (categoryConfig.handlers.isPresent()) {
                addNamedHandlersToCategory(categoryConfig, namedHandlers, categoryLogger, errorManager);
            }
        }
    }

    private static class AdditionalNamedHandlersConsumer implements BiConsumer<String, Handler> {
        private final Map<String, Handler> additionalNamedHandlersMap;
        private final ErrorManager errorManager;
        private final List<LogCleanupFilterElement> filterElements;

        public AdditionalNamedHandlersConsumer(Map<String, Handler> additionalNamedHandlersMap, ErrorManager errorManager,
                List<LogCleanupFilterElement> filterElements) {
            this.additionalNamedHandlersMap = additionalNamedHandlersMap;
            this.errorManager = errorManager;
            this.filterElements = filterElements;
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
            handler.setFilter(new LogCleanupFilter(filterElements));
        }
    }
}
