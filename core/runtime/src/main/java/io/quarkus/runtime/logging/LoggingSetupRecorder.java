package io.quarkus.runtime.logging;

import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;

/**
 *
 */
@Recorder
public class LoggingSetupRecorder {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /**
     * <a href="https://conemu.github.io">ConEmu</a> ANSI X3.64 support enabled,
     * used by <a href="https://cmder.net/">cmder</a>
     */
    private static final boolean IS_CON_EMU_ANSI = IS_WINDOWS && "ON".equals(System.getenv("ConEmuANSI"));

    /**
     * These tests are same as used in jansi
     * Source: https://github.com/fusesource/jansi/commit/bb3d538315c44f799d34fd3426f6c91c8e8dfc55
     */
    private static final boolean IS_CYGWIN = IS_WINDOWS
            && System.getenv("PWD") != null
            && System.getenv("PWD").startsWith("/")
            && !"cygwin".equals(System.getenv("TERM"));

    private static final boolean IS_MINGW_XTERM = IS_WINDOWS
            && System.getenv("MSYSTEM") != null
            && System.getenv("MSYSTEM").startsWith("MINGW")
            && "xterm".equals(System.getenv("TERM"));

    public LoggingSetupRecorder() {
    }

    @SuppressWarnings("unsed") //called via reflection, as it is in an isolated CL
    public static void handleFailedStart() {
        LogConfig config = new LogConfig();
        ConfigInstantiator.handleObject(config);
        new LoggingSetupRecorder().initializeLogging(config, Collections.emptyList(),
                Collections.emptyList());
    }

    public void initializeLogging(LogConfig config, final List<RuntimeValue<Optional<Handler>>> additionalHandlers,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {

        final Map<String, CategoryConfig> categories = config.categories;
        final LogContext logContext = LogContext.getLogContext();
        final Logger rootLogger = logContext.getLogger("");

        rootLogger.setLevel(config.level.orElse(Level.INFO));

        ErrorManager errorManager = new OnlyOnceErrorManager();
        final Map<String, CleanupFilterConfig> filters = config.filters;
        List<LogCleanupFilterElement> filterElements = new ArrayList<>(filters.size());
        for (Entry<String, CleanupFilterConfig> entry : filters.entrySet()) {
            filterElements.add(new LogCleanupFilterElement(entry.getKey(), entry.getValue().ifStartsWith));
        }

        final ArrayList<Handler> handlers = new ArrayList<>(3 + additionalHandlers.size());

        if (config.console.enable) {
            final Handler consoleHandler = configureConsoleHandler(config.console, errorManager, filterElements,
                    possibleFormatters);
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

        Map<String, Handler> namedHandlers = createNamedHandlers(config, possibleFormatters, errorManager, filterElements);

        for (Map.Entry<String, CategoryConfig> entry : categories.entrySet()) {
            final String name = entry.getKey();
            final Logger categoryLogger = logContext.getLogger(name);
            final CategoryConfig categoryConfig = entry.getValue();
            if (!"inherit".equals(categoryConfig.level)) {
                categoryLogger.setLevelName(categoryConfig.level);
            }
            categoryLogger.setUseParentHandlers(categoryConfig.useParentHandlers);
            if (categoryConfig.handlers.isPresent()) {
                addNamedHandlersToCategory(categoryConfig, namedHandlers, categoryLogger, errorManager);
            }
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

    private static Map<String, Handler> createNamedHandlers(LogConfig config,
            List<RuntimeValue<Optional<Formatter>>> possibleFormatters, ErrorManager errorManager,
            List<LogCleanupFilterElement> filterElements) {
        Map<String, Handler> namedHandlers = new HashMap<>();
        for (Entry<String, ConsoleConfig> consoleConfigEntry : config.consoleHandlers.entrySet()) {
            final Handler consoleHandler = configureConsoleHandler(consoleConfigEntry.getValue(), errorManager, filterElements,
                    possibleFormatters);
            addToNamedHandlers(namedHandlers, consoleHandler, consoleConfigEntry.getKey());
        }
        for (Entry<String, FileConfig> fileConfigEntry : config.fileHandlers.entrySet()) {
            final Handler fileHandler = configureFileHandler(fileConfigEntry.getValue(), errorManager, filterElements);
            addToNamedHandlers(namedHandlers, fileHandler, fileConfigEntry.getKey());
        }
        for (Entry<String, SyslogConfig> sysLogConfigEntry : config.syslogHandlers.entrySet()) {
            final Handler syslogHandler = configureSyslogHandler(sysLogConfigEntry.getValue(), errorManager, filterElements);
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
    }

    private void addNamedHandlersToCategory(CategoryConfig categoryConfig, Map<String, Handler> namedHandlers,
            Logger categoryLogger,
            ErrorManager errorManager) {
        for (String categoryNamedHandler : categoryConfig.handlers.get()) {
            if (namedHandlers.get(categoryNamedHandler) != null) {
                categoryLogger.addHandler(namedHandlers.get(categoryNamedHandler));
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

    private static boolean hasColorSupport() {

        if (IS_WINDOWS) {
            // On Windows without a known good emulator
            // TODO: optimally we would check if Win32 getConsoleMode has
            // ENABLE_VIRTUAL_TERMINAL_PROCESSING enabled or enable it via
            // setConsoleMode.
            // For now we turn it off to not generate noisy output for most
            // users.
            // Must be on some Unix variant or ANSI-enabled windows terminal...
            return IS_CON_EMU_ANSI || IS_CYGWIN || IS_MINGW_XTERM;
        } else {
            // on sane operating systems having a console is a good indicator
            // you are attached to a TTY with colors.
            return System.console() != null;
        }
    }

    private static Handler configureConsoleHandler(final ConsoleConfig config, final ErrorManager defaultErrorManager,
            final List<LogCleanupFilterElement> filterElements,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {
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
        if (formatter == null) {
            if (config.color.orElse(hasColorSupport())) {
                formatter = new ColorPatternFormatter(config.darken, config.format);
            } else {
                formatter = new PatternFormatter(config.format);
            }
        }
        final ConsoleHandler consoleHandler = new ConsoleHandler(formatter);
        consoleHandler.setLevel(config.level);
        consoleHandler.setErrorManager(defaultErrorManager);
        consoleHandler.setFilter(new LogCleanupFilter(filterElements));

        final Handler handler = config.async.enable ? createAsyncHandler(config.async, config.level, consoleHandler)
                : consoleHandler;

        if (formatterWarning) {
            handler.getErrorManager().error("Multiple formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }

        return handler;
    }

    private static Handler configureFileHandler(final FileConfig config, final ErrorManager errorManager,
            final List<LogCleanupFilterElement> filterElements) {
        FileHandler handler = new FileHandler();
        FileConfig.RotationConfig rotationConfig = config.rotation;
        if (rotationConfig.maxFileSize.isPresent() && rotationConfig.fileSuffix.isPresent()) {
            PeriodicSizeRotatingFileHandler periodicSizeRotatingFileHandler = new PeriodicSizeRotatingFileHandler();
            periodicSizeRotatingFileHandler.setSuffix(rotationConfig.fileSuffix.get());
            periodicSizeRotatingFileHandler.setRotateSize(rotationConfig.maxFileSize.get().asLongValue());
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

}
