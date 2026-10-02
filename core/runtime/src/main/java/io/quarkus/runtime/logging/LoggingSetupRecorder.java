package io.quarkus.runtime.logging;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
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
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextInitializer;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.errormanager.OnlyOnceErrorManager;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.dev.testing.ExceptionReporting;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.CategoryConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.CleanupFilterConfig;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Runtime entry points for legacy recorder-driven logging initialization.
 * <p>
 * The reusable handler-construction and orchestration logic now lives in {@link LoggingSetup}; this recorder
 * retains only the entry points that are still driven from bytecode recording or reflective fallbacks:
 * {@link #handleFailedStart()} (the failure fallback), {@link #initializeLogging} (invoked by the fallback),
 * {@link #initializeBuildTimeLogging} (invoked statically at build time), and
 * {@link #initializeLoggingForImageBuild()} (native image build). As logging is migrated to the service
 * scheme, these entry points shrink further.
 */
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
                        return config.getConfigValue(propertyName).getValue();
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
                        emptyList(), emptyList(), emptyList(), banner, LaunchMode.DEVELOPMENT, false);
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
            final List<RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>>> namedHandlerFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier,
            final LaunchMode launchMode,
            final boolean includeFilters) {

        LogBuildTimeConfig buildConfig = logBuildTimeConfig;
        LogRuntimeConfig config = logRuntimeConfig.getValue();

        LoggingSetup.ShutdownNotifier shutdownNotifier = new LoggingSetup.ShutdownNotifier();
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

        Map<String, Filter> namedFilters = LoggingSetup.createNamedFilters(discoveredLogComponents);
        ArrayList<Handler> handlers = new ArrayList<>(
                3 + additionalHandlers.size() + (config.handlers().isPresent() ? config.handlers().get().size() : 0));

        if (config.console().enabled()) {
            Handler consoleHandler = LoggingSetup.configureConsoleHandler(config.console(), consoleRuntimeConfig.getValue(),
                    errorManager,
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

        if (config.file().enabled()) {
            handlers.add(LoggingSetup.configureFileHandler(config.file(), errorManager, cleanupFiler, namedFilters,
                    possibleFileFormatters,
                    includeFilters));
        }

        if (config.syslog().enabled()) {
            Handler syslogHandler = LoggingSetup.configureSyslogHandler(config.syslog(), errorManager, cleanupFiler,
                    namedFilters,
                    possibleSyslogFormatters, includeFilters);
            if (syslogHandler != null) {
                handlers.add(syslogHandler);
            }
        }

        if (config.socket().enabled()) {
            final Handler socketHandler = LoggingSetup.configureSocketHandler(config.socket(), errorManager, cleanupFiler,
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

        Map<NamedHandlerType, Map<String, Optional<Formatter>>> mergedNamedHandlerFormatters = LoggingSetup
                .mergeNamedHandlerFormatters(namedHandlerFormatters);

        Map<String, Handler> namedHandlers = LoggingSetup.shouldCreateNamedHandlers(config, additionalNamedHandlers)
                ? LoggingSetup.createNamedHandlers(config, consoleRuntimeConfig.getValue(), additionalNamedHandlers,
                        mergedNamedHandlerFormatters,
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
                            new LoggingSetup.AdditionalNamedHandlersConsumer(additionalNamedHandlersMap, errorManager,
                                    filterElements,
                                    shutdownNotifier));
                }
            }

            namedHandlers.putAll(additionalNamedHandlersMap);

            LoggingSetup.setUpCategoryLoggers(buildConfig, categoryDefaultMinLevels, categories, logContext, errorManager,
                    namedHandlers,
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
        LoggingSetup.addNamedHandlersToRootHandlers(config.handlers(), namedHandlers, handlers, errorManager);
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setHandlers(handlers.toArray(LogContextInitializer.NO_HANDLERS));
        return shutdownNotifier;
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

        LoggingSetup.ShutdownNotifier dummy = new LoggingSetup.ShutdownNotifier();

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
        if (config.console().enabled()) {
            Handler consoleHandler = LoggingSetup.configureConsoleHandler(config.console(), consoleConfig, errorManager,
                    logCleanupFilter,
                    emptyMap(), emptyList(), new RuntimeValue<>(Optional.empty()), launchMode, false);
            errorManager = consoleHandler.getErrorManager();
            handlers.add(consoleHandler);
        }

        Map<String, Handler> namedHandlers = LoggingSetup.createNamedHandlers(config, consoleConfig, emptyList(),
                emptyMap(), emptyList(), emptyList(), emptyList(), emptyList(), errorManager, logCleanupFilter,
                emptyMap(), launchMode, dummy, false);

        LoggingSetup.setUpCategoryLoggers(buildConfig, categoryDefaultMinLevels, categories, logContext, errorManager,
                namedHandlers, false);

        LoggingSetup.addNamedHandlersToRootHandlers(config.handlers(), namedHandlers, handlers, errorManager);
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setBuildTimeHandlers(handlers.toArray(LogContextInitializer.NO_HANDLERS));
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
}
