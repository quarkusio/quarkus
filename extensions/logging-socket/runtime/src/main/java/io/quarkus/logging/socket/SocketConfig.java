package io.quarkus.logging.socket;

import java.util.Map;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.SocketHandler;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.handler.socket")
public class SocketConfig {
    /**
     * Determine whether to enable the Socket logger
     */
    @ConfigItem
    public boolean enabled;

    /**
     * Hostname/IP-Address of the remove logger host
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The port
     */
    @ConfigItem(defaultValue = "12201")
    public int port;

    /**
     * Sets protocol, possible values UDP, TCP, SSL_TCP
     */
    @ConfigItem(defaultValue = "UDP")
    public SocketHandler.Protocol protocol;

    /**
     * Whether to post block on socket reconnect
     *
     */
    @ConfigItem(defaultValue = "false")
    public boolean blockOnReconnect;

    /**
     * The logging-gelf log level.
     */
    @ConfigItem(defaultValue = "ALL")
    public Level level;

    /**
     * Handler formatter type, possible values: json, pattern
     */
    @ConfigItem(defaultValue = "json")
    public String formatter;

    /**
     * if formatter type is 'pattern', this value will be used for creation of a pattern
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    public String patternFormat;
    /**
     * comma-seperated key=value pairs
     * possible keys are:
     * EXCEPTION
     * EXCEPTION_CAUSED_BY
     * EXCEPTION_CIRCULAR_REFERENCE
     * EXCEPTION_TYPE
     * EXCEPTION_FRAME
     * EXCEPTION_FRAME_CLASS
     * EXCEPTION_FRAME_LINE
     * EXCEPTION_FRAME_METHOD
     * EXCEPTION_FRAMES
     * EXCEPTION_MESSAGE
     * EXCEPTION_REFERENCE_ID
     * EXCEPTION_SUPPRESSED
     * HOST_NAME
     * LEVEL
     * LOGGER_CLASS_NAME
     * LOGGER_NAME
     * MDC
     * MESSAGE
     * NDC
     * PROCESS_ID
     * PROCESS_NAME
     * RECORD
     * SEQUENCE
     * SOURCE_CLASS_NAME
     * SOURCE_FILE_NAME
     * SOURCE_LINE_NUMBER
     * SOURCE_METHOD_NAME
     * SOURCE_MODULE_NAME
     * SOURCE_MODULE_VERSION
     * STACK_TRACE
     * THREAD_ID
     * THREAD_NAME
     * TIMESTAMP
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
}
