package io.quarkus.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Core thread pool.
 * <p>
 * The core thread pool config. This thread pool is responsible for running
 * all blocking tasks.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ThreadPoolConfig {

    /**
     * The core thread pool size. This number of threads will always be kept alive.
     */
    @ConfigItem(defaultValue = "1")
    public int coreThreads;

    /**
     * Prefill core thread pool.
     * The core thread pool will be initialised with the core number of threads at startup
     */
    @ConfigItem(defaultValue = "true")
    public boolean prefill;

    /**
     * The maximum number of threads. If this is not specified then
     * it will be automatically sized to the greatest of 8 * the number of available processors and 200.
     * For example if there are 4 processors the max threads will be 200.
     * If there are 48 processors it will be 384.
     */
    @ConfigItem
    public OptionalInt maxThreads;

    /**
     * The queue size. For most applications this should be unbounded
     */
    @ConfigItem
    public OptionalInt queueSize;

    /**
     * The executor growth resistance.
     *
     * A resistance factor applied after the core pool is full; values applied here will cause that fraction
     * of submissions to create new threads when no idle thread is available. A value of {@code 0.0f} implies that
     * threads beyond the core size should be created as aggressively as threads within it; a value of {@code 1.0f}
     * implies that threads beyond the core size should never be created.
     */
    @ConfigItem
    public float growthResistance;

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then additional threads will be spawned to attempt to finish any pending tasks, and the shutdown process will
     * continue
     */
    @ConfigItem(defaultValue = "1M")
    public Duration shutdownTimeout;

    /**
     * The amount of time to wait for thread pool shutdown before tasks should be interrupted. If this value is
     * greater than or equal to the value for {@link #shutdownTimeout}, then tasks will not be interrupted before
     * the shutdown timeout occurs.
     */
    @ConfigItem(defaultValue = "10")
    public Duration shutdownInterrupt;

    /**
     * The frequency at which the status of the thread pool should be checked during shutdown. Information about
     * waiting tasks and threads will be checked and possibly logged at this interval. Setting this key to an empty
     * value disables the shutdown check interval.
     */
    @ConfigItem(defaultValue = "5")
    public Optional<Duration> shutdownCheckInterval;

    /**
     * The amount of time a thread will stay alive with no work.
     */
    @ConfigItem(defaultValue = "30")
    public Duration keepAliveTime;

    public static ThreadPoolConfig empty() {
        var config = new ThreadPoolConfig();
        config.maxThreads = OptionalInt.empty();
        config.queueSize = OptionalInt.empty();
        config.shutdownCheckInterval = Optional.empty();
        return config;
    }

}
