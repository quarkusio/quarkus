package io.quarkus.logging.json.structured;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.console.json.structured")
public class JsonStructuredConfig {
    /**
     * Configuration properties to customize fields
     */
    public FieldConfig fields;
    /**
     * Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "true")
    boolean enable;
    /**
     * Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read pretty printed output.
     */
    @ConfigItem
    boolean prettyPrint;
    /**
     * The date format to use. The special string "default" indicates that the default format should be used.
     */
    @ConfigItem(defaultValue = "default")
    String dateFormat;
    /**
     * The special end-of-record delimiter to be used. By default, newline delimiter is used.
     */
    @ConfigItem(defaultValue = "\n")
    String recordDelimiter;

    @ConfigGroup
    public static class FieldConfig {
        /**
         * Used to customize {@link io.quarkus.logging.json.structured.providers.ArgumentsJsonProvider}
         */
        public ArgumentsConfig arguments;
    }

    @ConfigGroup
    public static class ArgumentsConfig {

        /**
         * Used to wrap arguments in an json object, with this fieldName on root json.
         */
        @ConfigItem
        public Optional<String> fieldName;
        /**
         * Enable output of structured logging arguments
         * {@link io.quarkus.logging.json.structured.providers.StructuredArgument},
         * default is true.
         */
        @ConfigItem(defaultValue = "true")
        public boolean includeStructuredArguments;
        /**
         * Enable output of non structured logging arguments, default is false.
         */
        @ConfigItem(defaultValue = "false")
        public boolean includeNonStructuredArguments;
        /**
         * What prefix to use, when outputting non structured arguments. Default is `arg`, example key for first argument will
         * be `arg0`.
         */
        @ConfigItem(defaultValue = "arg")
        public String nonStructuredArgumentsFieldPrefix;
    }
}
