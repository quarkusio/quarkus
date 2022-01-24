package io.quarkus.runtime.logging;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.SocketHandler.Protocol;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SocketConfig {

    /**
     * If socket logging should be enabled
     */
    @ConfigItem
    boolean enable;

    /**
     *
     * The IP address and port of the server receiving the logs
     */
    @ConfigItem(defaultValue = "localhost:4560")
    InetSocketAddress endpoint;

    /**
     * Sets the protocol used to connect to the server
     */
    @ConfigItem(defaultValue = "tcp")
    Protocol protocol;

    /**
     * Enables or disables blocking when attempting to reconnect a
     * {@link Protocol#TCP
     * TCP} or {@link Protocol#SSL_TCP SSL TCP} protocol
     */
    @ConfigItem
    boolean blockOnReconnect;

    /**
     * The log message formatter. json or pattern is supported
     */
    @ConfigItem(defaultValue = "pattern")
    String formatter;

    /**
     * The log message format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;
    /**
     * The log level specifying, which message levels will be logged by socket logger
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;
    /**
     * comma-seperated key=value pairs
     * possible keys are listed {@link org.jboss.logmanager.formatters.StructuredFormatter.Key}
     * e.g. HOST_NAME=host,LEVEL=severity
     */
    @ConfigItem(defaultValue = "<NA>")
    public String keyoverrides;
    /**
     * Post additional fields only for JSON formatted messages
     * You can add static fields to each log event in the following form:
     *
     * <pre>
     * quarkus.log.handler.socket.additional-field.field1.value=value1
     * quarkus.log.handler.socket.additional-field.field1.type=String
     * </pre>
     */
    @ConfigItem
    @ConfigDocMapKey("field-name")
    @ConfigDocSection
    public Map<String, AdditionalFieldConfig> additionalField;

    /**
     * Syslog async logging config
     */
    AsyncConfig async;

    @ConfigGroup
    public class AdditionalFieldConfig {
        /**
         * Additional field value.
         */
        @ConfigItem
        public String value;

    }
}
