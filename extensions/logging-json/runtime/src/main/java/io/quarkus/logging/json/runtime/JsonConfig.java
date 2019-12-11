package io.quarkus.logging.json.runtime;

import java.util.Optional;

import org.jboss.logmanager.formatters.StructuredFormatter;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for JSON log formatting.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.console.json")
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
     * The special end-of-record delimiter to be used. By default, no delimiter is used.
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
}
