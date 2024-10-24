package io.quarkus.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Core thread pool.
 * <p>
 * The core thread pool config. This thread pool is responsible for running
 * all blocking tasks.
 */
@ConfigMapping(prefix = "quarkus.thread-pool")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ThreadPoolConfig {

    /**
     * The core thread pool size. This number of threads will always be kept alive.
     */
    @WithDefault("1")
    int coreThreads();

    /**
     * Prefill core thread pool.
     * The core thread pool will be initialised with the core number of threads at startup
     */
    @WithDefault("true")
    boolean prefill();

    /**
     * The maximum number of threads. If this is not specified then
     * it will be automatically sized to the greatest of 8 * the number of available processors and 200.
     * For example if there are 4 processors the max threads will be 200.
     * If there are 48 processors it will be 384.
     */
    OptionalInt maxThreads();

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
    @WithDefault("0.0")
    float growthResistance();

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then additional threads will be spawned to attempt to finish any pending tasks, and the shutdown process will
     * continue
     */
    @WithDefault("1M")
    Duration shutdownTimeout();

    /**
     * The amount of time to wait for thread pool shutdown before tasks should be interrupted. If this value is
     * greater than or equal to the value for {@link #shutdownTimeout}, then tasks will not be interrupted before
     * the shutdown timeout occurs.
     */
    @WithDefault("10")
    Duration shutdownInterrupt();

    /**
     * The frequency at which the status of the thread pool should be checked during shutdown. Information about
     * waiting tasks and threads will be checked and possibly logged at this interval. Setting this key to an empty
     * value disables the shutdown check interval.
     */
    @WithDefault("5")
    Optional<Duration> shutdownCheckInterval();

    /**
     * The amount of time a thread will stay alive with no work.
     */
    @WithDefault("30")
    Duration keepAliveTime();
}
