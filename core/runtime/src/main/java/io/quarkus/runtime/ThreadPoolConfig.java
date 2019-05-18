package io.quarkus.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
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
     * The maximum number of threads. If this is not specified then
     * it will be automatically sized to 4 * the number of available processors
     */
    @ConfigItem
    public OptionalInt maxThreads;

    /**
     * The queue size. For most applications this should be unbounded
     */
    @ConfigItem(defaultValue = "0")
    public int queueSize;

    /**
     * The executor growth resistance.
     *
     * A resistance factor applied after the core pool is full; values applied here will cause that fraction
     * of submissions to create new threads when no idle thread is available. A value of {@code 0.0f} implies that
     * threads beyond the core size should be created as aggressively as threads within it; a value of {@code 1.0f}
     * implies that threads beyond the core size should never be created.
     */
    @ConfigItem(defaultValue = "0")
    public float growthResistance;

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then additional threads will be spawned to attempt to finish any pending tasks, and the shutdown process will
     * continue
     */
    @ConfigItem(defaultValue = "PT60S")
    public Duration shutdownTimeout;

    /**
     * The amount of time to wait for thread pool shutdown before tasks should be interrupted. If this value is
     * greater than or equal to the value for {@link #shutdownTimeout}, then tasks will not be interrupted before
     * the shutdown timeout occurs.
     */
    @ConfigItem(defaultValue = "PT10S")
    public Duration shutdownInterrupt;

    /**
     * The frequency at which the status of the thread pool should be checked during shutdown. Information about
     * waiting tasks and threads will be checked and possibly logged at this interval. Setting this key to an empty
     * value disables the shutdown check interval.
     */
    @ConfigItem(defaultValue = "PT5S")
    public Optional<Duration> shutdownCheckInterval;

    /**
     * The amount of time a thread will stay alive with no work.
     */
    @ConfigItem(defaultValue = "PT30S")
    public Duration keepAliveTime;

}
