package io.quarkus.logging.json.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logmanager.formatters.StructuredFormatter;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * Configuration for JSON log formatting.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.log")
public interface JsonLogConfig extends LogRuntimeConfig {

    /**
     * Console logging.
     */
    @ConfigDocSection
    @WithName("console.json")
    JsonConfig consoleJson();

    /**
     * File logging.
     */
    @ConfigDocSection
    @WithName("file.json")
    JsonConfig fileJson();

    /**
     * Syslog logging.
     */
    @ConfigDocSection
    @WithName("syslog.json")
    JsonConfig syslogJson();

    /**
     * Socket logging.
     */
    @ConfigDocSection
    @WithName("socket.json")
    JsonConfig socketJson();

    /**
     * Console handlers.
     * <p>
     * The named console handlers configured here can be linked on one or more categories.
     */
    @WithName("handler.console")
    @ConfigDocSection
    Map<String, JsonConsoleConfig> jsonConsoleHandlers();

    /**
     * File handlers.
     * <p>
     * The named file handlers configured here can be linked on one or more categories.
     */
    @WithName("handler.file")
    @ConfigDocSection
    Map<String, JsonFileConfig> jsonFileHandlers();

    /**
     * Syslog handlers.
     * <p>
     * The named syslog handlers configured here can be linked on one or more categories.
     */
    @WithName("handler.syslog")
    @ConfigDocSection
    Map<String, JsonSyslogConfig> jsonSyslogHandlers();

    /**
     * Socket handlers.
     * <p>
     * The named socket handlers configured here can be linked on one or more categories.
     */
    @WithName("handler.socket")
    @ConfigDocSection
    Map<String, JsonSocketConfig> jsonSocketHandlers();

    @ConfigDocIgnore
    @Override
    Map<String, ConsoleConfig> consoleHandlers();

    @ConfigDocIgnore
    @Override
    Map<String, FileConfig> fileHandlers();

    @ConfigDocIgnore
    @Override
    Map<String, SyslogConfig> syslogHandlers();

    @ConfigDocIgnore
    @Override
    Map<String, SocketConfig> socketHandlers();

    interface JsonHandlerConfig {
        /**
         * JSON logging configuration.
         */
        @ConfigDocSection
        @WithName("json")
        JsonConfig json();
    }

    interface JsonConsoleConfig extends LogRuntimeConfig.ConsoleConfig, JsonHandlerConfig {
    }

    interface JsonFileConfig extends LogRuntimeConfig.FileConfig, JsonHandlerConfig {
    }

    interface JsonSyslogConfig extends LogRuntimeConfig.SyslogConfig, JsonHandlerConfig {
    }

    interface JsonSocketConfig extends LogRuntimeConfig.SocketConfig, JsonHandlerConfig {
    }

    @ConfigGroup
    interface JsonConfig {
        /**
         * Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.
         */
        @WithParentName
        @WithDefault("true")
        @Deprecated(forRemoval = true, since = "3.19")
        boolean enable();

        /**
         * Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.
         */
        // TODO make it non-optional with default true as soon as we drop the other config
        Optional<Boolean> enabled();

        /**
         * Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.
         */
        @WithDefault("false")
        boolean prettyPrint();

        /**
         * The date format to use. The special string "default" indicates that the default format should be used.
         */
        @WithDefault("default")
        String dateFormat();

        /**
         * The special end-of-record delimiter to be used. By default, newline is used.
         */
        Optional<String> recordDelimiter();

        /**
         * The zone ID to use. The special string "default" indicates that the default zone should be used.
         */
        @WithDefault("default")
        String zoneId();

        /**
         * The exception output type to specify.
         */
        @WithDefault("detailed")
        StructuredFormatter.ExceptionOutputType exceptionOutputType();

        /**
         * Enable printing of more details in the log.
         * <p>
         * Printing the details can be expensive as the values are retrieved from the caller. The details include the
         * source class name, source file name, source method name, and source line number.
         */
        @WithDefault("false")
        boolean printDetails();

        /**
         * Override keys with custom values. Omitting this value indicates that no key overrides will be applied.
         */
        Optional<String> keyOverrides();

        /**
         * Keys to be excluded from the JSON output.
         */
        Optional<Set<String>> excludedKeys();

        /**
         * Additional fields to be appended in the JSON logs.
         */
        @ConfigDocMapKey("field-name")
        Map<String, AdditionalFieldConfig> additionalField();

        /**
         * Specify the format of the produced JSON
         */
        @WithDefault("default")
        LogFormat logFormat();

        enum LogFormat {
            DEFAULT,
            ECS,
            GCP
        }
    }

    /**
     * Post additional fields. E.g. `fieldName1=value1,fieldName2=value2`.
     */
    @ConfigGroup
    interface AdditionalFieldConfig {
        /**
         * Additional field value.
         */
        String value();

        /**
         * Additional field type specification.
         * Supported types: {@code string}, {@code int}, and {@code long}.
         * String is the default if not specified.
         */
        @WithDefault("string")
        Type type();

        enum Type {
            STRING,
            INT,
            LONG
        }
    }
}
