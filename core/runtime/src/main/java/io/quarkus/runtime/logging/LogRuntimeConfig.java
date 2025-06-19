package io.quarkus.runtime.logging;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;
import org.jboss.logmanager.handlers.SocketHandler;
import org.jboss.logmanager.handlers.SyslogHandler.Facility;
import org.jboss.logmanager.handlers.SyslogHandler.Protocol;
import org.jboss.logmanager.handlers.SyslogHandler.SyslogType;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.configuration.InetSocketAddressConverter;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * Logging
 */
@ConfigMapping(prefix = "quarkus.log")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LogRuntimeConfig {
    /**
     * The log level of the root category, which is used as the default log level for all categories.
     * <p>
     * JBoss Logging supports Apache-style log levels:
     * <p>
     * * {@link org.jboss.logmanager.Level#FATAL}
     * * {@link org.jboss.logmanager.Level#ERROR}
     * * {@link org.jboss.logmanager.Level#WARN}
     * * {@link org.jboss.logmanager.Level#INFO}
     * * {@link org.jboss.logmanager.Level#DEBUG}
     * * {@link org.jboss.logmanager.Level#TRACE}
     *
     * In addition, it also supports the standard JDK log levels.
     *
     * @asciidoclet
     */
    @WithDefault("INFO")
    @WithConverter(LevelConverter.class)
    Level level();

    /**
     * Console logging.
     * <p>
     * Console logging is enabled by default.
     */
    @ConfigDocSection
    ConsoleConfig console();

    /**
     * File logging.
     * <p>
     * Logging to a file is also supported but not enabled by default.
     */
    @ConfigDocSection
    FileConfig file();

    /**
     * Syslog logging.
     * <p>
     * Logging to a syslog is also supported but not enabled by default.
     */
    @ConfigDocSection
    SyslogConfig syslog();

    /**
     * Socket logging.
     * <p>
     * Logging to a socket is also supported but not enabled by default.
     */
    @ConfigDocSection
    SocketConfig socket();

    /**
     * Logging categories.
     * <p>
     * Logging is done on a per-category basis. Each category can be independently configured.
     * A configuration that applies to a category will also apply to all sub-categories of that category,
     * unless there is a more specific matching sub-category configuration.
     */
    @WithName("category")
    @ConfigDocSection
    Map<String, CategoryConfig> categories();

    /**
     * Console handlers.
     * <p>
     * The named console handlers configured here can be linked on one or more categories.
     */
    @WithName("handler.console")
    @ConfigDocSection
    Map<String, ConsoleConfig> consoleHandlers();

    /**
     * File handlers.
     * <p>
     * The named file handlers configured here can be linked to one or more categories.
     */
    @WithName("handler.file")
    @ConfigDocSection
    Map<String, FileConfig> fileHandlers();

    /**
     * Syslog handlers.
     * <p>
     * The named syslog handlers configured here can be linked to one or more categories.
     */
    @WithName("handler.syslog")
    @ConfigDocSection
    Map<String, SyslogConfig> syslogHandlers();

    /**
     * Socket handlers.
     * <p>
     * The named socket handlers configured here can be linked to one or more categories.
     */
    @WithName("handler.socket")
    @ConfigDocSection
    Map<String, SocketConfig> socketHandlers();

    /**
     * Log cleanup filters - internal use.
     */
    @WithName("filter")
    @ConfigDocSection
    Map<String, CleanupFilterConfig> filters();

    /**
     * The names of additional handlers to link to the root category.
     * These handlers are defined in consoleHandlers, fileHandlers, or syslogHandlers.
     */
    Optional<List<String>> handlers();

    interface CategoryConfig {
        /**
         * The log level for this category.
         * <p>
         * Note that to get log levels below <code>INFO</code>,
         * the minimum level build-time configuration option also needs to be adjusted.
         */
        @WithDefault("inherit")
        InheritableLevel level();

        /**
         * The names of the handlers to link to this category.
         */
        Optional<List<String>> handlers();

        /**
         * Specify whether this logger should send its output to its parent Logger
         */
        @WithDefault("true")
        boolean useParentHandlers();
    }

    interface FileConfig {
        /**
         * Default file name where logs should be stored.
         */
        String DEFAULT_LOG_FILE_NAME = "quarkus.log";

        /**
         * If file logging should be enabled
         */
        @WithDefault("false")
        boolean enable();

        /**
         * The log format
         */
        @WithDefault("%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n")
        String format();

        /**
         * The level of logs to be written into the file.
         */
        @WithDefault("ALL")
        @WithConverter(LevelConverter.class)
        Level level();

        /**
         * The name of the file in which logs will be written.
         */
        @WithDefault(DEFAULT_LOG_FILE_NAME)
        File path();

        /**
         * The name of the filter to link to the file handler.
         */
        Optional<String> filter();

        /**
         * The character encoding used
         */
        Optional<@WithConverter(CharsetConverter.class) Charset> encoding();

        /**
         * File async logging config
         */
        AsyncConfig async();

        /**
         * File rotation config.
         * The time interval is determined by the content of the <code>fileSuffix</code> property.
         * The size interval is determined by the content of the <code>maxFileSize</code> property.
         * If both are used, the rotating will be based on time, then on size.
         */
        RotationConfig rotation();

        interface RotationConfig {

            /**
             * Whether log rotation is enabled.
             */
            @WithDefault("true")
            boolean enabled();

            /**
             * The maximum log file size, after which a rotation is executed, up to {@code Long.MAX_VALUE} bytes.
             * Note that the file is rotated <em>after</em> the log record is written.
             * Thus, this isn't a hard maximum on the file size; rather, it's a hard <em>minimum</em>
             * on the size of the file before it is rotated.
             */
            @WithDefault("10M")
            @WithConverter(MemorySizeConverter.class)
            MemorySize maxFileSize();

            /**
             * The maximum number of backups to keep.
             */
            @WithDefault("5")
            int maxBackupIndex();

            /**
             * The file handler rotation file suffix.
             * When used, the file will be rotated based on its suffix.
             * <p>
             * The suffix must be in a date-time format that is understood by {@link DateTimeFormatter}.
             * <p>
             * Example fileSuffix: .yyyy-MM-dd
             * <p>
             * Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.
             */
            Optional<String> fileSuffix();

            /**
             * Indicates whether to rotate log files on server initialization.
             * <p>
             * You need to either set a {@code max-file-size} or configure a {@code file-suffix} for it to work.
             */
            @WithDefault("true")
            boolean rotateOnBoot();
        }
    }

    interface ConsoleConfig {
        /**
         * If console logging should be enabled
         */
        @WithDefault("true")
        boolean enable();

        /**
         * If console logging should go to {@link System#err} instead of {@link System#out}.
         */
        @WithDefault("false")
        boolean stderr();

        /**
         * The log format. Note that this value is ignored if an extension is present that takes
         * control of console formatting (e.g., an XML or JSON-format extension).
         */
        @WithDefault("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
        String format();

        /**
         * The console log level.
         */
        @WithDefault("ALL")
        @WithConverter(LevelConverter.class)
        Level level();

        /**
         * If the console logging should be in color. If undefined, Quarkus takes
         * best guess based on the operating system and environment.
         * Note that this value is ignored if an extension is present that takes
         * control of console formatting (e.g., an XML or JSON-format extension).
         * <p>
         * This has been deprecated and replaced with <code>quarkus.console.color</code>,
         * as Quarkus now provides more console-based functionality than just logging.
         */
        @Deprecated
        Optional<Boolean> color();

        /**
         * Specify how much the colors should be darkened.
         * Note that this value is ignored if an extension is present that takes
         * control of console formatting (e.g., an XML or JSON-format extension).
         */
        @WithDefault("0")
        int darken();

        /**
         * The name of the filter to link to the console handler.
         */
        Optional<String> filter();

        /**
         * Console async logging config
         */
        AsyncConfig async();
    }

    interface SyslogConfig {
        /**
         * If syslog logging should be enabled
         */
        @WithDefault("false")
        boolean enable();

        /**
         *
         * The IP address and port of the Syslog server
         */
        @WithDefault("localhost:514")
        @WithConverter(InetSocketAddressConverter.class)
        InetSocketAddress endpoint();

        /**
         * The app name used when formatting the message in RFC5424 format
         */
        Optional<String> appName();

        /**
         * The name of the host the messages are being sent from
         */
        Optional<String> hostname();

        /**
         * Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164
         */
        @WithDefault("user-level")
        Facility facility();

        /**
         * Set the {@link SyslogType syslog type} this handler should use to format the message sent
         */
        @WithDefault("rfc5424")
        SyslogType syslogType();

        /**
         * Sets the protocol used to connect to the Syslog server
         */
        @WithDefault("tcp")
        Protocol protocol();

        /**
         * If enabled, the message being sent is prefixed with the size of the message
         */
        @WithDefault("protocol-dependent")
        CountingFraming useCountingFraming();

        /**
         * Set to {@code true} to truncate the message if it exceeds maximum length
         */
        @WithDefault("true")
        boolean truncate();

        /**
         * Enables or disables blocking when attempting to reconnect a
         * {@link Protocol#TCP
         * TCP} or {@link Protocol#SSL_TCP SSL TCP} protocol
         */
        @WithDefault("false")
        boolean blockOnReconnect();

        /**
         * The log message format
         */
        @WithDefault("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
        String format();

        /**
         * The log level specifying what message levels will be logged by the Syslog logger
         */
        @WithDefault("ALL")
        @WithConverter(LevelConverter.class)
        Level level();

        /**
         * The name of the filter to link to the file handler.
         */
        Optional<String> filter();

        /**
         * The maximum length, in bytes, of the message allowed to be sent, up to {@code Integer.MAX_VALUE} bytes. The length
         * includes the header and the message.
         * <p>
         * If not set, the default value is {@code 2048} when {@code sys-log-type} is {@code rfc5424} (which is the default)
         * and {@code 1024} when {@code sys-log-type} is {@code rfc3164}
         */
        Optional<@WithConverter(MemorySizeConverter.class) MemorySize> maxLength();

        /**
         * Syslog async logging config
         */
        AsyncConfig async();

        /**
         * Syslog counting framing type used for smarter handling of counting framing value.
         * <p>
         * If {@link CountingFraming#PROTOCOL_DEPENDENT} is used, the counting framing will be {@code true}, when the
         * {@link Protocol#TCP} or {@link Protocol#SSL_TCP} is used. Otherwise {@code false}.
         * <p>
         * More information in <a href="http://tools.ietf.org/html/rfc6587#section-3.4.1">http://tools.ietf.org/html/rfc6587</a>
         */
        enum CountingFraming {
            TRUE,
            FALSE,
            PROTOCOL_DEPENDENT
        }
    }

    interface SocketConfig {

        /**
         * If socket logging should be enabled
         */
        @WithDefault("false")
        boolean enable();

        /**
         *
         * The IP address and port of the server receiving the logs
         */
        @WithDefault("localhost:4560")
        @WithConverter(InetSocketAddressConverter.class)
        InetSocketAddress endpoint();

        /**
         * Sets the protocol used to connect to the syslog server
         */
        @WithDefault("tcp")
        SocketHandler.Protocol protocol();

        /**
         * Enables or disables blocking when attempting to reconnect a
         * {@link Protocol#TCP
         * TCP} or {@link Protocol#SSL_TCP SSL TCP} protocol
         */
        @WithDefault("false")
        boolean blockOnReconnect();

        /**
         * The log message format
         */
        @WithDefault("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
        String format();

        /**
         * The log level specifying, which message levels will be logged by socket logger
         */
        @WithDefault("ALL")
        Level level();

        /**
         * The name of the filter to link to the file handler.
         */
        Optional<String> filter();

        /**
         * Socket async logging config
         */
        AsyncConfig async();
    }

    interface CleanupFilterConfig {
        /**
         * The message prefix to match
         */
        @WithDefault("inherit")
        List<String> ifStartsWith();

        /**
         * The new log level for the filtered message. Defaults to DEBUG.
         */
        @WithDefault("DEBUG")
        @WithConverter(LevelConverter.class)
        Level targetLevel();
    }

    interface AsyncConfig {

        /**
         * Indicates whether to log asynchronously
         */
        @WithDefault("false")
        boolean enable();

        /**
         * Indicates whether to log asynchronously
         */
        @WithParentName
        @Deprecated(forRemoval = true, since = "3.24")
        Optional<Boolean> legacyEnable();

        /**
         * The queue length to use before flushing writing
         */
        @WithDefault("512")
        int queueLength();

        /**
         * Determine whether to block the publisher (rather than drop the message) when the queue is full
         */
        @WithDefault("block")
        OverflowAction overflow();
    }
}
