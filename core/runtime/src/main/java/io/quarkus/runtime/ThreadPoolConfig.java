package io.quarkus.runtime;

import java.time.Duration;

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
     * The maximum number of threads. If this is not specified or <= to zero then
     * it will be automatically sized to 4 * the number of available processors
     */
    @ConfigItem(defaultValue = "0")
    public int maxThreads;

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
     * The shutdown timeout in milliseconds. Defaults to 60s. If all pending work has not been completed by this time
     * then additional threads will be spawned to attempt to finish any pending tasks, and the shutdown process will
     * continue
     */
    @ConfigItem(defaultValue = "PT60S")
    public Duration shutdownTimeout;

    /**
     * The amount of time in milliseconds a thread will stay alive with no work. Defaults to 1 second
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration keepAliveTime;

}
