package io.quarkus.logging.gelf;

import java.util.Map;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.handler.gelf")
public class GelfConfig {
    /**
     * Determine whether to enable the GELF logging handler
     */
    @ConfigItem
    public boolean enabled;

    /**
     * Hostname/IP-Address of the Logstash/Graylog Host
     * By default it uses UDP, prepend tcp: to the hostname to switch to TCP, example: "tcp:localhost"
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The port
     */
    @ConfigItem(defaultValue = "12201")
    public int port;

    /**
     * GELF version: 1.0 or 1.1
     */
    @ConfigItem(defaultValue = "1.1")
    public String version;

    /**
     * Whether to post Stack-Trace to StackTrace field.
     * 
     * @see #stackTraceThrowableReference to customize the way the Stack-Trace is handled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean extractStackTrace;

    /**
     * Only used when `extractStackTrace` is `true`.
     * A value of 0 will extract the whole stack trace.
     * Any positive value will walk the cause chain: 1 corresponds with exception.getCause(),
     * 2 with exception.getCause().getCause(), ...
     * Negative throwable reference walk the exception chain from the root cause side: -1 will extract the root cause,
     * -2 the exception wrapping the root cause, ...
     */
    @ConfigItem(defaultValue = "0")
    public int stackTraceThrowableReference;

    /**
     * Whether to perform Stack-Trace filtering
     */
    @ConfigItem
    public boolean filterStackTrace;

    /**
     * Java date pattern, see {@link java.text.SimpleDateFormat}
     */
    @ConfigItem(defaultValue = "yyyy-MM-dd HH:mm:ss,SSS")
    public String timestampPattern;

    /**
     * The logging-gelf log level.
     */
    @ConfigItem(defaultValue = "ALL")
    public Level level;

    /**
     * Name of the facility.
     */
    @ConfigItem(defaultValue = "jboss-logmanager")
    public String facility;

    /**
     * Post additional fields.
     * You can add static fields to each log event in the following form:
     * 
     * <pre>
     * quarkus.log.handler.gelf.additional-field.field1.value=value1
     * quarkus.log.handler.gelf.additional-field.field1.type=String
     * </pre>
     */
    @ConfigItem
    @ConfigDocMapKey("field-name")
    @ConfigDocSection
    public Map<String, AdditionalFieldConfig> additionalField;

    /**
     * Whether to include all fields from the MDC.
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeFullMdc;
}
