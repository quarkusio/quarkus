package org.jboss.shamrock.jaeger.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

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
    public Optional<String> endpoint;

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
     * The hostname for communicating with agent via UDP
     */
    @ConfigItem
    public Optional<String> agentHost;

    /**
     * The port for communicating with agent via UDP
     */
    @ConfigItem
    public Optional<String> agentPort;

    /**
     * Whether the reporter should also log the spans
     */
    @ConfigItem
    public Optional<String> reporterLogSpans;

    /**
     * The reporter's maximum queue size
     */
    @ConfigItem
    public Optional<String> reporterMaxQueueSize;

    /**
     * The reporter's flush interval (ms)
     */
    @ConfigItem
    public Optional<String> reporterFlushInterval;

    /**
     * The sampler type (const, probabilistic, ratelimiting or remote)
     */
    @ConfigItem
    public Optional<String> samplerType;

    /**
     * The sampler parameter (number)
     */
    @ConfigItem
    public Optional<String> samplerParam;

    /**
     * The host name and port when using the remote controlled sampler
     */
    @ConfigItem
    public Optional<String> samplerManagerHostPort;

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

}
