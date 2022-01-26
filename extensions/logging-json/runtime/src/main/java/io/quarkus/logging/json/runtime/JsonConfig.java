package io.quarkus.logging.json.runtime;

import java.util.Map;
import java.util.Optional;

import org.jboss.logmanager.formatters.StructuredFormatter;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration for JSON log formatting.
 */
@ConfigGroup
public class JsonConfig {
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
     * The special end-of-record delimiter to be used. By default, newline is used as delimiter.
     */
    @ConfigItem
    Optional<String> recordDelimiter;
    /**
     * The zone ID to use. The special string "default" indicates that the default zone should be used.
     */
    @ConfigItem(defaultValue = "default")
    String zoneId;
    /**
     * The exception output type to specify.
     */
    @ConfigItem(defaultValue = "detailed")
    StructuredFormatter.ExceptionOutputType exceptionOutputType;
    /**
     * Enable printing of more details in the log.
     * <p>
     * Printing the details can be expensive as the values are retrieved from the caller. The details include the
     * source class name, source file name, source method name and source line number.
     */
    @ConfigItem
    boolean printDetails;

    /**
     * comma-seperated key=value pairs
     * possible keys are listed {@link StructuredFormatter.Key}
     * e.g. HOST_NAME=host,LEVEL=severity
     */
    @ConfigItem(defaultValue = "<NA>")
    public String keyoverrides;

    /**
     * Post additional fields only for JSON formatted messages
     * You can add static fields to each log event in the following form:
     *
     * <pre>
     * quarkus.log.handler.socket.mySocket.additional-field.field1.value=value1
     * quarkus.log.console.additional-field.field2.value=value2
     * </pre>
     */
    @ConfigItem
    @ConfigDocMapKey("field-name")
    @ConfigDocSection
    public Map<String, AdditionalFieldConfig> additionalField;

    @ConfigGroup
    public static class AdditionalFieldConfig {
        /**
         * Additional field value.
         */
        @ConfigItem
        public String value;

    }
}
