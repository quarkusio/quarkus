package io.quarkus.logging.json.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logmanager.formatters.StructuredFormatter;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * Configuration for JSON log formatting.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.log")
public interface JsonLogConfig {

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

    @ConfigGroup
    public interface JsonConfig {
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

        public enum LogFormat {
            DEFAULT,
            ECS
        }
    }

    /**
     * Post additional fields. E.g. `fieldName1=value1,fieldName2=value2`.
     */
    @ConfigGroup
    public interface AdditionalFieldConfig {
        /**
         * Additional field value.
         */
        public String value();

        /**
         * Additional field type specification.
         * Supported types: {@code string}, {@code int}, and {@code long}.
         * String is the default if not specified.
         */
        @WithDefault("string")
        public Type type();

        public enum Type {
            STRING,
            INT,
            LONG,
        }
    }
}
