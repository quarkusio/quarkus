package io.quarkus.jaeger.runtime;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The Jaeger configuration.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class JaegerConfig {

    /**
     * The traces endpoint, in case the client should connect directly to the Collector,
     * like http://jaeger-collector:14268/api/traces
     */
    @ConfigItem
    public Optional<URI> endpoint;

    /**
     * Authentication Token to send as "Bearer" to the endpoint
     */
    @ConfigItem
    public Optional<String> authToken;

    /**
     * Username to send as part of "Basic" authentication to the endpoint
     */
    @ConfigItem
    public Optional<String> user;

    /**
     * Password to send as part of "Basic" authentication to the endpoint
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The hostname and port for communicating with agent via UDP
     */
    @ConfigItem
    public Optional<InetSocketAddress> agentHostPort;

    /**
     * Whether the reporter should also log the spans
     */
    @ConfigItem
    public Optional<Boolean> reporterLogSpans;

    /**
     * The reporter's maximum queue size
     */
    @ConfigItem
    public OptionalInt reporterMaxQueueSize;

    /**
     * The reporter's flush interval
     */
    @ConfigItem
    public Optional<Duration> reporterFlushInterval;

    /**
     * The sampler type (const, probabilistic, ratelimiting or remote)
     */
    @ConfigItem
    public Optional<String> samplerType;

    /**
     * The sampler parameter (number)
     */
    @ConfigItem
    public Optional<BigDecimal> samplerParam;

    /**
     * The host name and port when using the remote controlled sampler
     */
    @ConfigItem
    public Optional<InetSocketAddress> samplerManagerHostPort;

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> serviceName;

    /**
     * A comma separated list of name = value tracer level tags, which get added to all reported
     * spans. The value can also refer to an environment variable using the format ${envVarName:default},
     * where the :default is optional, and identifies a value to be used if the environment variable
     * cannot be found
     */
    @ConfigItem
    public Optional<String> tags;

    /**
     * Comma separated list of formats to use for propagating the trace context. Defaults to the
     * standard Jaeger format. Valid values are jaeger and b3
     */
    @ConfigItem
    public Optional<String> propagation;

    /**
     * The sender factory class name
     */
    @ConfigItem
    public Optional<String> senderFactory;

    /**
     * Whether the trace context should be logged.
     */
    @ConfigItem(defaultValue = "true")
    public Boolean logTraceContext;

    /**
     * Whether or not the registration of tracer as the global tracer should be disabled.
     * This setting should only be turned on in tests that need to install a mock tracer.
     */
    @ConfigItem(defaultValue = "false")
    public Boolean disableTracerRegistration;

}
