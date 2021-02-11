package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class VertxConfiguration {

    /**
     * Enables or disables the Vert.x cache.
     */
    @ConfigItem(defaultValue = "true")
    public boolean caching;

    /**
     * Enables or disabled the Vert.x classpath resource resolver.
     */
    @ConfigItem(defaultValue = "true")
    public boolean classpathResolving;

    /**
     * The number of event loops. 2 x the number of core by default.
     */
    @ConfigItem
    public OptionalInt eventLoopsPoolSize;

    /**
     * The maximum amount of time the event loop can be blocked.
     */
    @ConfigItem(defaultValue = "2")
    public Duration maxEventLoopExecuteTime;

    /**
     * The amount of time before a warning is displayed if the event loop is blocked.
     */
    @ConfigItem(defaultValue = "2")
    public Duration warningExceptionTime;

    /**
     * The size of the worker thread pool.
     */
    @ConfigItem(defaultValue = "20")
    public int workerPoolSize;

    /**
     * The maximum amount of time the worker thread can be blocked.
     */
    @ConfigItem(defaultValue = "60")
    public Duration maxWorkerExecuteTime;

    /**
     * The size of the internal thread pool (used for the file system).
     */
    @ConfigItem(defaultValue = "20")
    public int internalBlockingPoolSize;

    /**
     * Enables the async DNS resolver.
     */
    @ConfigItem
    public boolean useAsyncDNS;

    /**
     * The event bus configuration.
     */
    @ConfigItem
    public EventBusConfiguration eventbus;

    /**
     * The cluster configuration.
     */
    @ConfigItem
    public ClusterConfiguration cluster;

    /**
     * The address resolver configuration.
     */
    @ConfigItem
    public AddressResolverConfiguration resolver;

    /**
     * Enable or disable native transport
     */
    @ConfigItem
    public boolean preferNativeTransport;

}
