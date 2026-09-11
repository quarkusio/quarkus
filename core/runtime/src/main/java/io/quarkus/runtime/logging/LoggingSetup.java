package io.quarkus.runtime.logging;

import static io.smallrye.common.net.HostName.getQualifiedHostName;
import static io.smallrye.common.os.Process.getProcessName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextInitializer;
import org.jboss.logmanager.Logger;
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
import io.quarkus.dev.console.TerminalUtils;
import io.quarkus.dev.testing.ExceptionReporting;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.LogBuildTimeConfig.CategoryBuildTimeConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.CategoryConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.ConsoleConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.FileConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig.SocketConfig;
import io.quarkus.runtime.shutdown.ShutdownListener;

/**
 * Construction helpers for the individual {@link Handler}s used by the Quarkus logging subsystem.
 * <p>
 * These helpers are shared between the runtime logging service actions, the build-time logging
 * initialization path, and the legacy {@link LoggingSetupRecorder}. Each helper is a pure factory: it
 * builds and configures a handler from the given configuration and collaborators, and never mutates any
 * shared global state.
 * <p>
 * The primary handler factory methods take a fully resolved {@link Formatter} (which may be {@code null} to
 * request the default formatter for the handler type). The overloads that accept a list of
 * {@link RuntimeValue}-wrapped candidate formatters exist only to support the legacy bytecode recorder
 * scheme and are marked accordingly.
 */
public final class LoggingSetup {

    private static final org.jboss.logging.Logger log = org.jboss.logging.Logger
            .getLogger(LoggingSetup.class.getPackageName());

    /**
     * A shared error manager that silently discards all errors. Used as the default error manager for
     * non-console handlers when the console handler is disabled, since in that case there is no reasonable
     * path for error reporting.
     */
    private static final ErrorManager DISCARDING_ERROR_MANAGER = new ErrorManager() {
        @Override
        public void error(final String msg, final Exception ex, final int code) {
            // discard: there is no reasonable path for error reporting
        }
    };

    private LoggingSetup() {
    }

    /**
     * {@return the shared error manager that silently discards all errors}
     * This is used as the default error manager for non-console handlers when the console handler is
     * disabled.
     */
    public static ErrorManager discardingErrorManager() {
        return DISCARDING_ERROR_MANAGER;
    }

    /**
     * Build and configure a console handler using the given resolved formatter.
     *
     * @param config the console handler configuration (must not be {@code null})
     * @param consoleRuntimeConfig the console runtime configuration (must not be {@code null})
     * @param defaultErrorManager the error manager to install on the handler (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param formatter the resolved formatter to use, or {@code null} to build the default formatter
     * @param bannerSupplier the banner supplier to prepend to the default formatter, or {@code null} for none
     * @param launchMode the current launch mode (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured console handler (not {@code null})
     */
    public static Handler configureConsoleHandler(
            final ConsoleConfig config,
            final ConsoleRuntimeConfig consoleRuntimeConfig,
            final ErrorManager defaultErrorManager,
            final LogCleanupFilter cleanupFilter,
            final Map<String, Filter> namedFilters,
            Formatter formatter,
            final Supplier<String> bannerSupplier,
            LaunchMode launchMode,
            boolean includeFilters) {
        ConsoleHandlerResult result = buildConsoleHandler(config, consoleRuntimeConfig, defaultErrorManager, cleanupFilter,
                namedFilters, formatter, bannerSupplier, includeFilters);
        return wrapConsoleHandler(result, config, launchMode);
    }

    /**
     * The result of building a raw console handler: the handler itself and whether color output is enabled
     * (which governs whether the dev/test exception-highlighting wrapper is applied by
     * {@link #wrapConsoleHandler(ConsoleHandlerResult, ConsoleConfig, LaunchMode)}).
     *
     * @param handler the raw console handler (never {@code null})
     * @param color {@code true} if color output is enabled for the handler
     */
    public record ConsoleHandlerResult(ConsoleHandler handler, boolean color) {
    }

    /**
     * Build a raw console handler, without any async or color-highlighting wrapping.
     * <p>
     * The returned {@link ConsoleHandler} exposes {@link ConsoleHandler#getLocalErrorManager()}, which is used
     * as the default error manager for other handlers. Apply async and dev-mode wrapping via
     * {@link #wrapConsoleHandler(ConsoleHandlerResult, ConsoleConfig, LaunchMode)} before attaching the handler
     * to a logger.
     *
     * @param config the console handler configuration (must not be {@code null})
     * @param consoleRuntimeConfig the console runtime configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param formatter the resolved formatter to use, or {@code null} to build the default formatter
     * @param bannerSupplier the banner supplier to prepend to the default formatter, or {@code null} for none
     * @param includeFilters {@code true} if named filters should be applied
     * @return the raw console handler and its color flag (not {@code null})
     */
    public static ConsoleHandlerResult buildConsoleHandler(
            final ConsoleConfig config,
            final ConsoleRuntimeConfig consoleRuntimeConfig,
            final ErrorManager errorManager,
            final LogCleanupFilter cleanupFilter,
            final Map<String, Filter> namedFilters,
            Formatter formatter,
            final Supplier<String> bannerSupplier,
            boolean includeFilters) {
        boolean color = false;
        if (formatter == null) {
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
        consoleHandler.setErrorManager(errorManager);
        applyFilter(includeFilters, errorManager, cleanupFilter, config.filter(), namedFilters, consoleHandler);
        return new ConsoleHandlerResult(consoleHandler, color);
    }

    /**
     * Wrap a raw console handler with async and/or dev-mode color-highlighting wrapping, as configured.
     *
     * @param result the raw console handler and its color flag (must not be {@code null})
     * @param config the console handler configuration (must not be {@code null})
     * @param launchMode the current launch mode (must not be {@code null})
     * @return the wrapped console handler ready to attach to a logger (not {@code null})
     */
    public static Handler wrapConsoleHandler(final ConsoleHandlerResult result, final ConsoleConfig config,
            final LaunchMode launchMode) {
        final ConsoleHandler consoleHandler = result.handler();
        boolean asyncEnabled = config.async().enabled();

        Handler handler = asyncEnabled
                ? createAsyncHandler(config.async(), config.level(), consoleHandler)
                : consoleHandler;

        if (result.color() && launchMode.isDevOrTest() && !asyncEnabled) {
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

        return handler;
    }

    /**
     * Build and configure a console handler from legacy recorder-contributed candidate formatters.
     * <p>
     * RECORDER COMPAT: resolves the candidate formatter/banner {@link RuntimeValue}s (warning if more than
     * one formatter was contributed) and then delegates to
     * {@link #configureConsoleHandler(ConsoleConfig, ConsoleRuntimeConfig, ErrorManager, LogCleanupFilter, Map, Formatter, Supplier, LaunchMode, boolean)}.
     *
     * @param config the console handler configuration (must not be {@code null})
     * @param consoleRuntimeConfig the console runtime configuration (must not be {@code null})
     * @param defaultErrorManager the error manager to install on the handler (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param possibleFormatters the candidate formatters contributed by extensions (must not be {@code null})
     * @param possibleBannerSupplier the optional banner supplier, or {@code null} if none
     * @param launchMode the current launch mode (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured console handler (not {@code null})
     */
    public static Handler configureConsoleHandler(
            final ConsoleConfig config,
            final ConsoleRuntimeConfig consoleRuntimeConfig,
            final ErrorManager defaultErrorManager,
            final LogCleanupFilter cleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters,
            final RuntimeValue<Optional<Supplier<String>>> possibleBannerSupplier,
            LaunchMode launchMode,
            boolean includeFilters) {
        FormatterChoice choice = resolveFormatterChoice(possibleFormatters);
        Supplier<String> bannerSupplier = null;
        if (possibleBannerSupplier != null && possibleBannerSupplier.getValue().isPresent()) {
            bannerSupplier = possibleBannerSupplier.getValue().get();
        }
        Handler handler = configureConsoleHandler(config, consoleRuntimeConfig, defaultErrorManager, cleanupFilter,
                namedFilters, choice.formatter(), bannerSupplier, launchMode, includeFilters);
        if (choice.multiple()) {
            handler.getErrorManager().error("Multiple console formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }
        return handler;
    }

    /**
     * Build and configure a file handler using the given resolved formatter.
     *
     * @param config the file handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param formatter the resolved formatter to use, or {@code null} to build the default formatter
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured file handler, possibly wrapped in an async handler (not {@code null})
     */
    public static Handler configureFileHandler(final FileConfig config, final ErrorManager errorManager,
            final LogCleanupFilter cleanupFilter, Map<String, Filter> namedFilters,
            Formatter formatter,
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

        if (config.async().enabled()) {
            return createAsyncHandler(config.async(), config.level(), handler);
        }
        return handler;
    }

    /**
     * Build and configure a file handler from legacy recorder-contributed candidate formatters.
     * <p>
     * RECORDER COMPAT: resolves the candidate formatter {@link RuntimeValue}s (warning if more than one
     * formatter was contributed) and then delegates to
     * {@link #configureFileHandler(FileConfig, ErrorManager, LogCleanupFilter, Map, Formatter, boolean)}.
     *
     * @param config the file handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param possibleFileFormatters the candidate formatters contributed by extensions (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured file handler, possibly wrapped in an async handler (not {@code null})
     */
    public static Handler configureFileHandler(final FileConfig config, final ErrorManager errorManager,
            final LogCleanupFilter cleanupFilter, Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters,
            final boolean includeFilters) {
        FormatterChoice choice = resolveFormatterChoice(possibleFileFormatters);
        Handler handler = configureFileHandler(config, errorManager, cleanupFilter, namedFilters, choice.formatter(),
                includeFilters);
        if (choice.multiple()) {
            handler.getErrorManager().error("Multiple file formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }
        return handler;
    }

    /**
     * Build and configure a syslog handler using the given resolved formatter.
     *
     * @param config the syslog handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param logCleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param formatter the resolved formatter to use, or {@code null} to build the default formatter
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured syslog handler, or {@code null} if it could not be created
     */
    public static Handler configureSyslogHandler(final LogRuntimeConfig.SyslogConfig config,
            final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            Formatter formatter,
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
                MemorySize maxLen = config.maxLength().get();
                int maxLenInt;
                if (maxLen.compareTo(Integer.MAX_VALUE) > 0) {
                    errorManager.error(
                            "Using 2GB as the value of maxLength for SyslogHandler as it is the maximum allowed value", null,
                            ErrorManager.GENERIC_FAILURE);
                    maxLenInt = Integer.MAX_VALUE;
                } else if (maxLen.compareTo(128) < 0) {
                    errorManager.error(
                            "Using 128 as the value of maxLength for SyslogHandler as using a smaller value is not allowed",
                            null, ErrorManager.GENERIC_FAILURE);
                    maxLenInt = 128;
                } else {
                    maxLenInt = maxLen.asIntValue();
                }
                handler.setMaxLength(maxLenInt);
            }

            if (formatter == null) {
                formatter = new PatternFormatter(config.format());
            }
            handler.setFormatter(formatter);

            handler.setErrorManager(errorManager);
            handler.setFilter(logCleanupFilter);
            applyFilter(includeFilters, errorManager, logCleanupFilter, config.filter(), namedFilters, handler);

            if (config.async().enabled()) {
                return createAsyncHandler(config.async(), config.level(), handler);
            }
            return handler;
        } catch (IOException e) {
            errorManager.error("Failed to create syslog handler", e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }

    /**
     * Build and configure a syslog handler from legacy recorder-contributed candidate formatters.
     * <p>
     * RECORDER COMPAT: resolves the candidate formatter {@link RuntimeValue}s (warning if more than one
     * formatter was contributed) and then delegates to
     * {@link #configureSyslogHandler(LogRuntimeConfig.SyslogConfig, ErrorManager, LogCleanupFilter, Map, Formatter, boolean)}.
     *
     * @param config the syslog handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param logCleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param possibleSyslogFormatters the candidate formatters contributed by extensions (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured syslog handler, or {@code null} if it could not be created
     */
    public static Handler configureSyslogHandler(final LogRuntimeConfig.SyslogConfig config,
            final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters,
            final boolean includeFilters) {
        FormatterChoice choice = resolveFormatterChoice(possibleSyslogFormatters);
        Handler handler = configureSyslogHandler(config, errorManager, logCleanupFilter, namedFilters, choice.formatter(),
                includeFilters);
        if (handler != null && choice.multiple()) {
            handler.getErrorManager().error("Multiple syslog formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }
        return handler;
    }

    /**
     * Build and configure a socket handler using the given resolved formatter.
     *
     * @param config the socket handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param logCleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param formatter the resolved formatter to use, or {@code null} to build the default formatter
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured socket handler, or {@code null} if it could not be created
     */
    public static Handler configureSocketHandler(final SocketConfig config,
            final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            Formatter formatter,
            final boolean includeFilters) {
        try {
            final SocketHandler handler = new SocketHandler(config.endpoint().getHostString(), config.endpoint().getPort());
            handler.setProtocol(config.protocol());
            handler.setBlockOnReconnect(config.blockOnReconnect());
            handler.setLevel(config.level());

            if (formatter == null) {
                formatter = new PatternFormatter(config.format());
            }
            handler.setFormatter(formatter);

            handler.setErrorManager(errorManager);
            handler.setFilter(logCleanupFilter);
            applyFilter(includeFilters, errorManager, logCleanupFilter, config.filter(), namedFilters, handler);

            if (config.async().enabled()) {
                return createAsyncHandler(config.async(), config.level(), handler);
            }
            return handler;
        } catch (IOException e) {
            errorManager.error("Failed to create socket handler", e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }

    /**
     * Build and configure a socket handler from legacy recorder-contributed candidate formatters.
     * <p>
     * RECORDER COMPAT: resolves the candidate formatter {@link RuntimeValue}s (warning if more than one
     * formatter was contributed) and then delegates to
     * {@link #configureSocketHandler(SocketConfig, ErrorManager, LogCleanupFilter, Map, Formatter, boolean)}.
     *
     * @param config the socket handler configuration (must not be {@code null})
     * @param errorManager the error manager to install on the handler (must not be {@code null})
     * @param logCleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param possibleSocketFormatters the candidate formatters contributed by extensions (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the configured socket handler, or {@code null} if it could not be created
     */
    public static Handler configureSocketHandler(final SocketConfig config,
            final ErrorManager errorManager,
            final LogCleanupFilter logCleanupFilter,
            final Map<String, Filter> namedFilters,
            final List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters,
            final boolean includeFilters) {
        FormatterChoice choice = resolveFormatterChoice(possibleSocketFormatters);
        Handler handler = configureSocketHandler(config, errorManager, logCleanupFilter, namedFilters, choice.formatter(),
                includeFilters);
        if (handler != null && choice.multiple()) {
            handler.getErrorManager().error("Multiple socket formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }
        return handler;
    }

    /**
     * The result of resolving a list of candidate formatters: the selected formatter (the last present one,
     * or {@code null} if none) and whether more than one candidate was present.
     * <p>
     * RECORDER COMPAT: this record, and all of its usages, exist only to support the legacy recorder scheme
     * where multiple candidate formatters may be contributed for a single handler type. It can be removed
     * together with the list-based {@code configure*Handler} overloads once recorders are gone.
     *
     * @param formatter the selected formatter, or {@code null} if none was present
     * @param multiple {@code true} if more than one candidate formatter was present
     */
    private record FormatterChoice(Formatter formatter, boolean multiple) {
    }

    /**
     * Resolve a list of candidate formatters to a single choice.
     * <p>
     * RECORDER COMPAT: this list-based resolution exists only for the legacy recorder scheme, where
     * multiple extensions may each contribute a formatter for the same handler type. In the service scheme,
     * a formatter is a uniquely named service, so at most one can exist per handler type.
     *
     * @param possibleFormatters the candidate formatters (must not be {@code null})
     * @return the formatter choice (not {@code null})
     */
    private static FormatterChoice resolveFormatterChoice(final List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {
        Formatter formatter = null;
        boolean multiple = false;
        for (RuntimeValue<Optional<Formatter>> value : possibleFormatters) {
            if (formatter != null) {
                multiple = true;
            }
            final Optional<Formatter> val = value.getValue();
            if (val.isPresent()) {
                formatter = val.get();
            }
        }
        return new FormatterChoice(formatter, multiple);
    }

    /**
     * Apply the appropriate filter to the given handler. When named filters are enabled and the handler
     * configuration references a named filter, the handler is given a composite filter that applies both the
     * cleanup filter and the named filter; otherwise, only the cleanup filter is applied.
     *
     * @param includeFilters {@code true} if named filters should be applied
     * @param errorManager the error manager to report failures to (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param filterName the optional configured named filter name (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param handler the handler to configure (must not be {@code null})
     */
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

    /**
     * Wrap the given handler in an asynchronous handler.
     *
     * @param asyncConfig the async configuration (must not be {@code null})
     * @param level the level to set on the async handler (must not be {@code null})
     * @param handler the handler to wrap (must not be {@code null})
     * @return the async handler wrapping the given handler (not {@code null})
     */
    private static AsyncHandler createAsyncHandler(LogRuntimeConfig.AsyncConfig asyncConfig, Level level, Handler handler) {
        final AsyncHandler asyncHandler = new AsyncHandler(asyncConfig.queueLength());
        asyncHandler.setOverflowAction(asyncConfig.overflow());
        asyncHandler.addHandler(handler);
        asyncHandler.setLevel(level);
        return asyncHandler;
    }

    /**
     * Determine whether color output is enabled for the console.
     *
     * @param consoleConfig the console runtime configuration (must not be {@code null})
     * @param logConfig the console handler configuration (must not be {@code null})
     * @return {@code true} if color output should be enabled
     */
    private static boolean isColorEnabled(ConsoleRuntimeConfig consoleConfig, ConsoleConfig logConfig) {
        if (consoleConfig.color().isPresent()) {
            return consoleConfig.color().get();
        }
        return TerminalUtils.hasColorSupport();
    }

    /**
     * Create the map of named filters (from {@code @LoggingFilter}-annotated classes) discovered at build time.
     *
     * @param discoveredLogComponents the discovered log components (must not be {@code null})
     * @return a map of filter name to filter instance (not {@code null})
     */
    public static Map<String, Filter> createNamedFilters(DiscoveredLogComponents discoveredLogComponents) {
        if (discoveredLogComponents.getNameToFilterClass().isEmpty()) {
            return Map.of();
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
     * Determine whether named handlers need to be created.
     *
     * @param logRuntimeConfig the log runtime configuration (must not be {@code null})
     * @param additionalNamedHandlers the additional named handlers contributed by extensions (must not be {@code null})
     * @return {@code true} if named handlers should be created
     */
    public static boolean shouldCreateNamedHandlers(
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

    /**
     * Resolve the effective log level for a category, walking up the category hierarchy until a non-inherited
     * level is found or the root is reached.
     *
     * @param categoryName the category name (must not be {@code null})
     * @param categories the category configurations, keyed by category name (must not be {@code null})
     * @param levelExtractor a function extracting the inheritable level from a category configuration (must not be
     *        {@code null})
     * @param categoryDefaults default levels per category (must not be {@code null})
     * @param rootMinLevel the root minimum level to use if no configured level is found (must not be {@code null})
     * @param <T> the category configuration type
     * @return the resolved level (not {@code null})
     */
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

    /**
     * Resolve the configured level for a category without walking up the category hierarchy.
     *
     * @param categoryName the category name (must not be {@code null})
     * @param categories the category configurations, keyed by category name (must not be {@code null})
     * @param levelExtractor a function extracting the inheritable level from a category configuration (must not be
     *        {@code null})
     * @param categoryDefaults default levels per category (must not be {@code null})
     * @param <T> the category configuration type
     * @return the configured inheritable level, or {@link InheritableLevel.Inherited#INSTANCE} if none is configured
     */
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

    /**
     * Merge the per-named-handler formatters contributed by extensions.
     * <p>
     * RECORDER COMPAT: takes {@link RuntimeValue}-wrapped formatter maps contributed by legacy recorders.
     *
     * @param namedHandlerFormatters the contributed named-handler formatter maps (must not be {@code null})
     * @return the merged map of formatters per handler type and handler name (not {@code null})
     */
    public static Map<NamedHandlerType, Map<String, Optional<Formatter>>> mergeNamedHandlerFormatters(
            List<RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>>> namedHandlerFormatters) {
        if (namedHandlerFormatters.isEmpty()) {
            return Map.of();
        }
        Map<NamedHandlerType, Map<String, Optional<Formatter>>> merged = new EnumMap<>(NamedHandlerType.class);
        for (RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>> rv : namedHandlerFormatters) {
            rv.getValue().forEach((type, formatters) -> {
                Map<String, Optional<Formatter>> typeMap = merged.computeIfAbsent(type, k -> new HashMap<>());
                formatters.forEach((name, formatter) -> {
                    if (typeMap.putIfAbsent(name, formatter) != null) {
                        log.warnf("Multiple formatters configured for named %s handler '%s', the last one will be used", type,
                                name);
                        typeMap.put(name, formatter);
                    }
                });
            });
        }
        return merged;
    }

    /**
     * Resolve the candidate formatters for a named handler, preferring a formatter configured specifically for
     * that handler and falling back to the global formatters for the handler type.
     * <p>
     * RECORDER COMPAT: returns {@link RuntimeValue}-wrapped candidate formatters for the legacy recorder scheme.
     *
     * @param handlerName the named handler name (must not be {@code null})
     * @param namedFormatters the formatters configured per named handler (must not be {@code null})
     * @param globalFormatters the global formatters for the handler type (must not be {@code null})
     * @return the candidate formatters to use for the named handler (not {@code null})
     */
    public static List<RuntimeValue<Optional<Formatter>>> resolveFormatters(
            String handlerName,
            Map<String, Optional<Formatter>> namedFormatters,
            List<RuntimeValue<Optional<Formatter>>> globalFormatters) {
        if (namedFormatters.containsKey(handlerName)) {
            return List.of(new RuntimeValue<>(namedFormatters.get(handlerName)));
        }
        // Fall back to the global formatter (e.g. JSON enabled globally applies to named handlers too).
        return globalFormatters;
    }

    /**
     * Create the named handlers configured under {@code quarkus.log.handler.*}, plus any additional named
     * handlers contributed by extensions.
     *
     * @param config the log runtime configuration (must not be {@code null})
     * @param consoleRuntimeConfig the console runtime configuration (must not be {@code null})
     * @param additionalNamedHandlers additional named handlers contributed by extensions (must not be {@code null})
     * @param namedHandlerFormatters formatters configured per named handler (must not be {@code null})
     * @param possibleConsoleFormatters the global console formatters (must not be {@code null})
     * @param possibleFileFormatters the global file formatters (must not be {@code null})
     * @param possibleSyslogFormatters the global syslog formatters (must not be {@code null})
     * @param possibleSocketFormatters the global socket formatters (must not be {@code null})
     * @param errorManager the error manager to install on the handlers (must not be {@code null})
     * @param cleanupFilter the log cleanup filter to apply (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param launchMode the current launch mode (must not be {@code null})
     * @param shutdownHandler the shutdown notifier (must not be {@code null})
     * @param includeFilters {@code true} if named filters should be applied
     * @return the map of named handlers, keyed by handler name (not {@code null})
     */
    public static Map<String, Handler> createNamedHandlers(
            LogRuntimeConfig config, ConsoleRuntimeConfig consoleRuntimeConfig,
            List<RuntimeValue<Map<String, Handler>>> additionalNamedHandlers,
            Map<NamedHandlerType, Map<String, Optional<Formatter>>> namedHandlerFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleConsoleFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleFileFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleSyslogFormatters,
            List<RuntimeValue<Optional<Formatter>>> possibleSocketFormatters,
            ErrorManager errorManager, LogCleanupFilter cleanupFilter,
            Map<String, Filter> namedFilters, LaunchMode launchMode,
            ShutdownNotifier shutdownHandler, boolean includeFilters) {
        Map<String, Optional<Formatter>> namedConsoleFormatters = namedHandlerFormatters.getOrDefault(NamedHandlerType.CONSOLE,
                Map.of());
        Map<String, Optional<Formatter>> namedFileFormatters = namedHandlerFormatters.getOrDefault(NamedHandlerType.FILE,
                Map.of());
        Map<String, Optional<Formatter>> namedSyslogFormatters = namedHandlerFormatters.getOrDefault(NamedHandlerType.SYSLOG,
                Map.of());
        Map<String, Optional<Formatter>> namedSocketFormatters = namedHandlerFormatters.getOrDefault(NamedHandlerType.SOCKET,
                Map.of());

        Map<String, Handler> namedHandlers = new HashMap<>();
        for (Entry<String, ConsoleConfig> consoleConfigEntry : config.consoleHandlers().entrySet()) {
            ConsoleConfig namedConsoleConfig = consoleConfigEntry.getValue();
            if (!namedConsoleConfig.enabled()) {
                continue;
            }
            final Handler consoleHandler = configureConsoleHandler(namedConsoleConfig, consoleRuntimeConfig,
                    errorManager, cleanupFilter, namedFilters,
                    resolveFormatters(consoleConfigEntry.getKey(), namedConsoleFormatters, possibleConsoleFormatters),
                    null, launchMode, includeFilters);
            addToNamedHandlers(namedHandlers, consoleHandler, consoleConfigEntry.getKey());
        }
        for (Entry<String, FileConfig> fileConfigEntry : config.fileHandlers().entrySet()) {
            FileConfig namedFileConfig = fileConfigEntry.getValue();
            if (!namedFileConfig.enabled()) {
                continue;
            }
            final Handler fileHandler = configureFileHandler(namedFileConfig, errorManager, cleanupFilter,
                    namedFilters,
                    resolveFormatters(fileConfigEntry.getKey(), namedFileFormatters, possibleFileFormatters), includeFilters);
            addToNamedHandlers(namedHandlers, fileHandler, fileConfigEntry.getKey());
        }
        for (Entry<String, LogRuntimeConfig.SyslogConfig> sysLogConfigEntry : config.syslogHandlers().entrySet()) {
            LogRuntimeConfig.SyslogConfig namedSyslogConfig = sysLogConfigEntry.getValue();
            if (!namedSyslogConfig.enabled()) {
                continue;
            }
            final Handler syslogHandler = configureSyslogHandler(namedSyslogConfig, errorManager, cleanupFilter,
                    namedFilters,
                    resolveFormatters(sysLogConfigEntry.getKey(), namedSyslogFormatters, possibleSyslogFormatters),
                    includeFilters);
            if (syslogHandler != null) {
                addToNamedHandlers(namedHandlers, syslogHandler, sysLogConfigEntry.getKey());
            }
        }
        for (Entry<String, SocketConfig> socketConfigEntry : config.socketHandlers().entrySet()) {
            SocketConfig namedSocketConfig = socketConfigEntry.getValue();
            if (!namedSocketConfig.enabled()) {
                continue;
            }
            final Handler socketHandler = configureSocketHandler(namedSocketConfig, errorManager, cleanupFilter,
                    namedFilters,
                    resolveFormatters(socketConfigEntry.getKey(), namedSocketFormatters, possibleSocketFormatters),
                    includeFilters);
            if (socketHandler != null) {
                addToNamedHandlers(namedHandlers, socketHandler, socketConfigEntry.getKey());
            }
        }

        Map<String, Handler> additionalNamedHandlersMap;
        if (additionalNamedHandlers.isEmpty()) {
            additionalNamedHandlersMap = Map.of();
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

    /**
     * Add a handler to the map of named handlers, registering a close task for it, and failing if a handler
     * with the same name already exists.
     *
     * @param namedHandlers the map of named handlers to add to (must not be {@code null})
     * @param handler the handler to add (must not be {@code null})
     * @param handlerName the handler name (must not be {@code null})
     */
    public static void addToNamedHandlers(Map<String, Handler> namedHandlers, Handler handler, String handlerName) {
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

    /**
     * Link the named handlers configured on a category to that category's logger, registering a removal task
     * for each linked handler.
     *
     * @param categoryConfig the category configuration (must not be {@code null})
     * @param namedHandlers the map of named handlers, keyed by handler name (must not be {@code null})
     * @param categoryLogger the category logger to link the handlers to (must not be {@code null})
     * @param errorManager the error manager to report missing handler links to (must not be {@code null})
     * @param checkHandlerLinks {@code true} if a missing handler link should be reported as an error
     */
    public static void addNamedHandlersToCategory(
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

    /**
     * Configure the per-category loggers: their levels (with minimum-level promotion), parent-handler usage,
     * and linked named handlers.
     *
     * @param buildConfig the log build-time configuration (must not be {@code null})
     * @param categoryDefaultMinLevels default minimum levels per category (must not be {@code null})
     * @param categories the category configurations, keyed by category name (must not be {@code null})
     * @param logContext the log context (must not be {@code null})
     * @param errorManager the error manager to report missing handler links to (must not be {@code null})
     * @param namedHandlers the map of named handlers, keyed by handler name (must not be {@code null})
     * @param checkHandlerLinks {@code true} if a missing handler link should be reported as an error
     */
    public static void setUpCategoryLoggers(
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

            Level logLevel = getLogLevel(categoryName, categories, CategoryConfig::level, Map.of(), buildConfig.minLevel());
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

    /**
     * Add the named handlers linked to the root category (via {@code quarkus.log.handlers}) to the given list
     * of effective root handlers.
     *
     * @param handlerNames the optional list of root handler names (must not be {@code null})
     * @param namedHandlers the map of named handlers, keyed by handler name (must not be {@code null})
     * @param effectiveHandlers the list of effective root handlers to add to (must not be {@code null})
     * @param errorManager the error manager to report missing handler links to (must not be {@code null})
     */
    public static void addNamedHandlersToRootHandlers(Optional<List<String>> handlerNames, Map<String, Handler> namedHandlers,
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

    /**
     * A shutdown listener that tracks whether shutdown has begun, so that the log cleanup filter can stop
     * suppressing records once the application is shutting down.
     */
    public static class ShutdownNotifier implements ShutdownListener {
        volatile boolean shutdown;

        @Override
        public void shutdown(ShutdownNotification notification) {
            shutdown = true;
            notification.done();
        }

        /**
         * Mark that shutdown has begun. Used by the logging configuration service's stop handler so that the
         * cleanup filter stops suppressing records once the application is shutting down.
         */
        public void markShutdown() {
            shutdown = true;
        }
    }

    /**
     * Create the dev/test exception-reporting handler, which forwards thrown exceptions from log records to
     * {@link ExceptionReporting} (used by the continuous testing and dev-mode error reporting infrastructure).
     *
     * @return the exception-reporting handler (not {@code null})
     */
    public static Handler createExceptionReportingHandler() {
        return new ExtHandler() {
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
        };
    }

    /**
     * Build the shared log cleanup filter from the runtime {@code quarkus.log.filter.*} configuration.
     *
     * @param config the log runtime configuration (must not be {@code null})
     * @param notifier the shutdown notifier the filter consults during shutdown (must not be {@code null})
     * @return the cleanup filter (not {@code null})
     */
    public static LogCleanupFilter buildCleanupFilter(LogRuntimeConfig config, ShutdownNotifier notifier) {
        Map<String, LogRuntimeConfig.CleanupFilterConfig> filters = config.filters();
        List<LogCleanupFilterElement> filterElements;
        if (filters.isEmpty()) {
            filterElements = List.of();
        } else {
            filterElements = new ArrayList<>(filters.size());
            filters.forEach((loggerName, filterConfig) -> filterElements
                    .add(new LogCleanupFilterElement(loggerName, filterConfig.targetLevel(), filterConfig.ifStartsWith())));
        }
        return new LogCleanupFilter(filterElements, notifier);
    }

    /**
     * Perform the final, whole-application logging configuration at runtime: set the root level, build the
     * named handlers and category loggers, assemble the effective root handler set from the handlers produced
     * by the individual handler services, and install them on the delayed handler.
     * <p>
     * The individual root handlers (console, file, syslog, socket, and the dev/test exception-reporting
     * handler) are produced by their own services and passed in via {@code serviceRootHandlers}; they are
     * already fully configured. Extension-contributed handlers, the streaming handler, and the per-named-handler
     * formatters are passed in via the {@code // RECORDER COMPAT} bridge wrappers.
     *
     * @param buildConfig the log build-time configuration (must not be {@code null})
     * @param config the log runtime configuration (must not be {@code null})
     * @param consoleRuntimeConfig the console runtime configuration (must not be {@code null})
     * @param categoryDefaultMinLevels default minimum levels per category (must not be {@code null})
     * @param notifier the shutdown notifier (must not be {@code null})
     * @param cleanupFilter the shared cleanup filter (must not be {@code null})
     * @param errorManager the shared default error manager (must not be {@code null})
     * @param namedFilters the map of named filters, keyed by filter name (must not be {@code null})
     * @param serviceRootHandlers the enabled root handlers produced by the handler services (must not be {@code null})
     * @param additionalHandlers extension-contributed root handlers (must not be {@code null})
     * @param streamingHandler the Dev UI streaming handler, or {@code null} if none
     * @param consoleFormatter the global console formatter for named-handler fallback, or {@code null}
     * @param fileFormatter the global file formatter for named-handler fallback, or {@code null}
     * @param syslogFormatter the global syslog formatter for named-handler fallback, or {@code null}
     * @param socketFormatter the global socket formatter for named-handler fallback, or {@code null}
     * @param namedHandlerFormatters the per-named-handler formatters, or {@code null} if none
     * @param banner the console banner, or {@code null} if none
     * @param launchMode the current launch mode (must not be {@code null})
     * @param enableWebStream {@code true} if the web log stream is always enabled
     * @param includeFilters {@code true} if named filters should be applied
     */
    public static void configureRuntimeLogging(
            final LogBuildTimeConfig buildConfig,
            final LogRuntimeConfig config,
            final ConsoleRuntimeConfig consoleRuntimeConfig,
            final Map<String, InheritableLevel> categoryDefaultMinLevels,
            final ShutdownNotifier notifier,
            final LogCleanupFilter cleanupFilter,
            final ErrorManager errorManager,
            final Map<String, Filter> namedFilters,
            final Collection<Handler> serviceRootHandlers,
            final Collection<AdditionalLogHandler> additionalHandlers,
            final StreamingLogHandler streamingHandler,
            final Formatter consoleFormatter,
            final Formatter fileFormatter,
            final Formatter syslogFormatter,
            final Formatter socketFormatter,
            final NamedHandlerFormatters namedHandlerFormatters,
            final LogBanner banner,
            final LaunchMode launchMode,
            final boolean enableWebStream,
            final boolean includeFilters) {

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

        // apply the cleanup filter to the handlers already installed on the root logger
        for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
            handler.setFilter(cleanupFilter);
        }

        ArrayList<Handler> handlers = new ArrayList<>(
                serviceRootHandlers.size() + additionalHandlers.size() + 2);
        // handlers produced by the individual handler services (console, file, syslog, socket, dev-exception);
        // these are already fully configured by their own services
        handlers.addAll(serviceRootHandlers);

        // the Dev UI streaming handler (RECORDER COMPAT bridge), configured here to match the previous behavior
        if ((launchMode.isDevOrTest() || enableWebStream) && streamingHandler != null) {
            Handler handler = streamingHandler.handler();
            handler.setErrorManager(errorManager);
            handler.setFilter(new LogCleanupFilter(cleanupFilter.filterElements.values(), notifier));
            if (banner != null) {
                String header = "\n" + banner.supplier().get();
                handler.publish(new LogRecord(Level.INFO, header));
            }
            handlers.add(handler);
        }

        // extension-contributed root handlers (RECORDER COMPAT bridge), configured here to match previous behavior
        for (AdditionalLogHandler additionalHandler : additionalHandlers) {
            Handler handler = additionalHandler.handler();
            handler.setErrorManager(errorManager);
            handler.setFilter(cleanupFilter);
            handlers.add(handler);
        }

        Map<NamedHandlerType, Map<String, Optional<Formatter>>> mergedNamedHandlerFormatters = namedHandlerFormatters == null
                ? Map.of()
                : namedHandlerFormatters.formatters();

        Map<String, Handler> namedHandlers = shouldCreateNamedHandlers(config, List.of())
                ? createNamedHandlers(config, consoleRuntimeConfig, List.of(), mergedNamedHandlerFormatters,
                        toFormatterList(consoleFormatter), toFormatterList(fileFormatter), toFormatterList(syslogFormatter),
                        toFormatterList(socketFormatter), errorManager, cleanupFilter, namedFilters, launchMode, notifier,
                        includeFilters)
                : Map.of();

        if (!config.categories().isEmpty()) {
            setUpCategoryLoggers(buildConfig, categoryDefaultMinLevels, config.categories(), logContext, errorManager,
                    namedHandlers, true);
        }

        addNamedHandlersToRootHandlers(config.handlers(), namedHandlers, handlers, errorManager);
        InitialConfigurator.DELAYED_HANDLER.setAutoFlush(false);
        InitialConfigurator.DELAYED_HANDLER.setHandlers(handlers.toArray(LogContextInitializer.NO_HANDLERS));
    }

    /**
     * Adapt a single resolved formatter to the list-of-candidate-formatters shape expected by
     * {@link #createNamedHandlers}.
     * <p>
     * RECORDER COMPAT: exists only to feed resolved formatter service values into the legacy list-based
     * named-handler construction; removable once {@code createNamedHandlers} takes resolved formatters.
     *
     * @param formatter the resolved formatter, or {@code null}
     * @return a singleton candidate list, or an empty list if {@code formatter} is {@code null}
     */
    private static List<RuntimeValue<Optional<Formatter>>> toFormatterList(Formatter formatter) {
        return formatter == null ? List.of() : List.of(new RuntimeValue<>(Optional.of(formatter)));
    }

    /**
     * A consumer that adds extension-contributed named handlers to a map, installing the shared error manager
     * and a fresh cleanup filter on each.
     * <p>
     * RECORDER COMPAT: consumes named handlers contributed by legacy recorders via build items.
     */
    static class AdditionalNamedHandlersConsumer implements BiConsumer<String, Handler> {
        private final Map<String, Handler> additionalNamedHandlersMap;
        private final ErrorManager errorManager;
        private final Collection<LogCleanupFilterElement> filterElements;

        private final ShutdownNotifier shutdownNotifier;

        /**
         * Construct a new instance.
         *
         * @param additionalNamedHandlersMap the map to add handlers to (must not be {@code null})
         * @param errorManager the error manager to install on each handler (must not be {@code null})
         * @param filterElements the cleanup filter elements to apply to each handler (must not be {@code null})
         * @param shutdownNotifier the shutdown notifier for the cleanup filter (must not be {@code null})
         */
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
}
