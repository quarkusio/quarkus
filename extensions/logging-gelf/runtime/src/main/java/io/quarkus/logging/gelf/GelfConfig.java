package io.quarkus.logging.gelf;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.log.handler.gelf")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface GelfConfig {
    /**
     * Determine whether to enable the GELF logging handler
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Hostname/IP-Address of the Logstash/Graylog Host
     * By default it uses UDP, prepend tcp: to the hostname to switch to TCP, example: "tcp:localhost"
     */
    @WithDefault("localhost")
    String host();

    /**
     * The port
     */
    @WithDefault("12201")
    int port();

    /**
     * GELF version: 1.0 or 1.1
     */
    @WithDefault("1.1")
    String version();

    /**
     * Whether to post Stack-Trace to StackTrace field.
     *
     * @see #stackTraceThrowableReference to customize the way the Stack-Trace is handled.
     */
    @WithDefault("true")
    boolean extractStackTrace();

    /**
     * Only used when `extractStackTrace` is `true`.
     * A value of 0 will extract the whole stack trace.
     * Any positive value will walk the cause chain: 1 corresponds with exception.getCause(),
     * 2 with exception.getCause().getCause(), ...
     * Negative throwable reference walk the exception chain from the root cause side: -1 will extract the root cause,
     * -2 the exception wrapping the root cause, ...
     */
    @WithDefault("0")
    int stackTraceThrowableReference();

    /**
     * Whether to perform Stack-Trace filtering
     */
    @WithDefault("false")
    boolean filterStackTrace();

    /**
     * Java date pattern, see {@link java.text.SimpleDateFormat}
     */
    @WithDefault("yyyy-MM-dd HH:mm:ss,SSS")
    String timestampPattern();

    /**
     * The logging-gelf log level.
     */
    @WithDefault("ALL")
    Level level();

    /**
     * Name of the facility.
     */
    @WithDefault("jboss-logmanager")
    String facility();

    /**
     * Post additional fields.
     * You can add static fields to each log event in the following form:
     *
     * <pre>
     * quarkus.log.handler.gelf.additional-field.field1.value=value1
     * quarkus.log.handler.gelf.additional-field.field1.type=String
     * </pre>
     */
    @ConfigDocSection
    @ConfigDocMapKey("field-name")
    Map<String, AdditionalFieldConfig> additionalField();

    /**
     * Whether to include all fields from the MDC.
     */
    @WithDefault("false")
    boolean includeFullMdc();

    /**
     * Send additional fields whose values are obtained from MDC. Name of the Fields are comma-separated. Example:
     * mdcFields=Application,Version,SomeOtherFieldName
     */
    Optional<String> mdcFields();

    /**
     * Dynamic MDC Fields allows you to extract MDC values based on one or more regular expressions. Multiple regexes are
     * comma-separated. The name of the MDC entry is used as GELF field name.
     */
    Optional<String> dynamicMdcFields();

    /**
     * Pattern-based type specification for additional and MDC fields. Key-value pairs are comma-separated. Example:
     * my_field.*=String,business\..*\.field=double
     */
    Optional<String> dynamicMdcFieldTypes();

    /**
     * Maximum message size (in bytes).
     * If the message size is exceeded, the appender will submit the message in multiple chunks.
     */
    @WithDefault("8192")
    int maximumMessageSize();

    /**
     * Include message parameters from the log event
     */
    @WithDefault("true")
    boolean includeLogMessageParameters();

    /**
     * Include source code location
     */
    @WithDefault("true")
    boolean includeLocation();

    /**
     * Origin hostname
     */
    Optional<String> originHost();

    /**
     * Bypass hostname resolution. If you didn't set the {@code originHost} property, and resolution is disabled, the value
     * “unknown” will be used as hostname
     */
    @WithDefault("false")
    boolean skipHostnameResolution();
}
