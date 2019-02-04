package org.jboss.shamrock.jaeger.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigGroup
@ConfigRoot(phase = ConfigPhase.STATIC_INIT)
public class JaegerConfig {

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> endpoint;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> authToken;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> user;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> agentHost;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> agentPort;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> reporterLogSpans;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> reporterMaxQueueSize;

    /**
     * The endpoint
     */
    @ConfigItem
    public Optional<String> reporterFlushInterval;

    /**
     * The sampler type
     */
    @ConfigItem
    public Optional<String> samplerType;

    /**
     * The sampler paramater
     */
    @ConfigItem
    public Optional<String> samplerParam;

    /**
     * The sampler paramater
     */
    @ConfigItem
    public Optional<String> samplerManagerHostPort;

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> serviceName;

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> tags;

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> propagation;

    /**
     * The service name
     */
    @ConfigItem
    public Optional<String> senderFactory;

}
