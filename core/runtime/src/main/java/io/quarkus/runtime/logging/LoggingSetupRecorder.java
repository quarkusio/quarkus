package io.quarkus.runtime.logging;

import static org.wildfly.common.net.HostName.getQualifiedHostName;
import static org.wildfly.common.os.Process.getProcessName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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

/**
 *
 */
@Recorder
public class LoggingSetupRecorder {

    static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /**
     * <a href="https://conemu.github.io">ConEmu</a> ANSI X3.64 support enabled,
     * used by <a href="https://cmder.net/">cmder</a>
     */
    static final boolean IS_CON_EMU_ANSI = IS_WINDOWS && "ON".equals(System.getenv("ConEmuANSI"));

    /**
     * These tests are same as used in jansi
     * Source: https://github.com/fusesource/jansi/commit/bb3d538315c44f799d34fd3426f6c91c8e8dfc55
     */
    static final boolean IS_CYGWIN = IS_WINDOWS
            && System.getenv("PWD") != null
            && System.getenv("PWD").startsWith("/")
            && !"cygwin".equals(System.getenv("TERM"));

    static final boolean IS_MINGW_XTERM = IS_WINDOWS
            && System.getenv("MSYSTEM") != null
            && System.getenv("MSYSTEM").startsWith("MINGW")
            && "xterm".equals(System.getenv("TERM"));

    public LoggingSetupRecorder() {
    }

    public void initializeLogging(LogConfig config, final List<RuntimeValue<Optional<Handler>>> additionalHandlers,
            final List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {

        final Map<String, CategoryConfig> categories = config.categories;
        final LogContext logContext = LogContext.getLogContext();
        final Logger rootLogger = logContext.getLogger("");
        ErrorManager errorManager = new OnlyOnceErrorManager();
        rootLogger.setLevel(config.level.orElse(Level.INFO));
        for (Map.Entry<String, CategoryConfig> entry : categories.entrySet()) {
            final String name = entry.getKey();
            final Logger logger = logContext.getLogger(name);
            final CategoryConfig categoryConfig = entry.getValue();
            if (!"inherit".equals(categoryConfig.level)) {
                logger.setLevelName(categoryConfig.level);
            }
        }
        final Map<String, CleanupFilterConfig> filters = config.filters;
        List<LogCleanupFilterElement> filterElements = new ArrayList<>(filters.size());
        for (Entry<String, CleanupFilterConfig> entry : filters.entrySet()) {
            filterElements.add(new LogCleanupFilterElement(entry.getKey(), entry.getValue().ifStartsWith));
        }
        ArrayList<Handler> handlers = new ArrayList<>(3 + additionalHandlers.size());
        if (config.console.enable) {
            errorManager = configureConsoleHandler(config.console, errorManager, filterElements, handlers, possibleFormatters);
        }

        if (config.file.enable) {
            configureFileHandler(config.file, errorManager, filterElements, handlers);
        }

        if (config.syslog.enable) {
            configureSyslogHandler(config.syslog, errorManager, filterElements, handlers);
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

        InitialConfigurator.DELAYED_HANDLER.setHandlers(handlers.toArray(EmbeddedConfigurator.NO_HANDLERS));
    }

    private boolean hasColorSupport() {

        if (IS_WINDOWS) {
            if (!(IS_CON_EMU_ANSI || IS_CYGWIN || IS_MINGW_XTERM)) {
                // On Windows without a known good emulator
                // TODO: optimally we would check if Win32 getConsoleMode has
                // ENABLE_VIRTUAL_TERMINAL_PROCESSING enabled or enable it via 
                // setConsoleMode.
                // For now we turn it off to not generate noisy output for most
                // users.
                return false;
            } else {
                // Must be on some Unix variant or ANSI-enabled windows terminal...
                return true;
            }
        } else {
            // on sane operating systems having a console is a good indicator
            // you are attached to a TTY with colors.
            return System.console() != null;
        }
    }

    private ErrorManager configureConsoleHandler(ConsoleConfig config, ErrorManager errorManager,
            List<LogCleanupFilterElement> filterElements, ArrayList<Handler> handlers,
            List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {
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
            if (config.color.orElse(Boolean.valueOf(hasColorSupport())).booleanValue()) {
                formatter = new ColorPatternFormatter(config.darken, config.format);
            } else {
                formatter = new PatternFormatter(config.format);
            }
        }
        final ConsoleHandler handler = new ConsoleHandler(formatter);
        handler.setLevel(config.level);
        handler.setErrorManager(errorManager);
        handler.setFilter(new LogCleanupFilter(filterElements));
        if (config.async.enable) {
            final AsyncHandler asyncHandler = new AsyncHandler(config.async.queueLength);
            asyncHandler.setOverflowAction(config.async.overflow);
            asyncHandler.addHandler(handler);
            asyncHandler.setLevel(config.level);
            handlers.add(asyncHandler);
        } else {
            handlers.add(handler);
        }
        errorManager = handler.getLocalErrorManager();
        if (formatterWarning) {
            errorManager.error("Multiple formatters were activated", null, ErrorManager.GENERIC_FAILURE);
        }
        return errorManager;
    }

    private void configureFileHandler(FileConfig config, ErrorManager errorManager,
            List<LogCleanupFilterElement> filterElements, ArrayList<Handler> handlers) {
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
            final AsyncHandler asyncHandler = new AsyncHandler(config.async.queueLength);
            asyncHandler.setOverflowAction(config.async.overflow);
            asyncHandler.addHandler(handler);
            asyncHandler.setLevel(config.level);
            handlers.add(asyncHandler);
        } else {
            handlers.add(handler);
        }
    }

    private void configureSyslogHandler(SyslogConfig config, ErrorManager errorManager,
            List<LogCleanupFilterElement> filterElements, ArrayList<Handler> handlers) {
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
                final AsyncHandler asyncHandler = new AsyncHandler(config.async.queueLength);
                asyncHandler.setOverflowAction(config.async.overflow);
                asyncHandler.addHandler(handler);
                asyncHandler.setLevel(config.level);
                handlers.add(asyncHandler);
            } else {
                handlers.add(handler);
            }
        } catch (IOException e) {
            errorManager.error("Failed to create syslog handler", e, ErrorManager.OPEN_FAILURE);
        }
    }

    public void initializeLoggingForImageBuild() {
        if (ImageInfo.inImageBuildtimeCode()) {
            final ConsoleHandler handler = new ConsoleHandler(new PatternFormatter(
                    "%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n"));
            handler.setLevel(Level.INFO);
            InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] { handler });
        }
    }
}
