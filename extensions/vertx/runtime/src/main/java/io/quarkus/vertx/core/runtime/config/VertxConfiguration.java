package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.vertx")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxConfiguration {

    /**
     * Enables or disables the Vert.x cache.
     */
    @WithDefault("true")
    boolean caching();

    /**
     * Configure the file cache directory.
     * When not set, the cache is stored in the system temporary directory (read from the `java.io.tmpdir` system property).
     * If the `java.io.tmpdir` is not set `.` is used.
     * <p>
     * Note that this property is ignored if the `vertx.cacheDirBase` system property is set.
     */
    Optional<String> cacheDirectory();

    /**
     * Enables or disabled the Vert.x classpath resource resolver.
     */
    @WithDefault("true")
    boolean classpathResolving();

    /**
     * The number of event loops. By default, it matches the number of CPUs detected on the system.
     */
    OptionalInt eventLoopsPoolSize();

    /**
     * The maximum amount of time the event loop can be blocked.
     */
    @WithDefault("2")
    Duration maxEventLoopExecuteTime();

    /**
     * The amount of time before a warning is displayed if the event loop is blocked.
     */
    @WithDefault("2")
    Duration warningExceptionTime();

    /**
     * @deprecated use {@code quarkus.thread-pool.max-threads} instead
     */
    @WithDefault("${quarkus.thread-pool.max-threads:20}")
    @Deprecated
    int workerPoolSize();

    /**
     * The maximum amount of time the worker thread can be blocked.
     */
    @WithDefault("60")
    Duration maxWorkerExecuteTime();

    /**
     * The size of the internal thread pool (used for the file system).
     */
    @WithDefault("20")
    int internalBlockingPoolSize();

    /**
     * The queue size. For most applications this should be unbounded
     */
    OptionalInt queueSize();

    /**
     * The executor growth resistance.
     * <p>
     * A resistance factor applied after the core pool is full; values applied here will cause that fraction
     * of submissions to create new threads when no idle thread is available. A value of {@code 0.0f} implies that
     * threads beyond the core size should be created as aggressively as threads within it; a value of {@code 1.0f}
     * implies that threads beyond the core size should never be created.
     */
    @WithDefault("0")
    float growthResistance();

    /**
     * The amount of time a thread will stay alive with no work.
     */
    @WithDefault("30")
    Duration keepAliveTime();

    /**
     * Prefill thread pool when creating a new Executor.
     * When {@link io.vertx.core.spi.ExecutorServiceFactory#createExecutor} is called,
     * initialise with the number of defined threads at startup
     */
    @WithDefault("false")
    boolean prefill();

    /**
     * Enables the async DNS resolver.
     */
    @WithDefault("false")
    boolean useAsyncDNS();

    /**
     * The event bus configuration.
     */
    EventBusConfiguration eventbus();

    /**
     * The cluster configuration.
     */
    ClusterConfiguration cluster();

    /**
     * The address resolver configuration.
     */
    AddressResolverConfiguration resolver();

    /**
     * Enable or disable native transport
     */
    @WithDefault("false")
    boolean preferNativeTransport();

}
