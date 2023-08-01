package io.quarkus.virtual.threads;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class VirtualThreadsConfig {

    /**
     * Virtual thread name prefix. If left blank virtual threads will be unnamed.
     */
    @ConfigItem(defaultValue = "quarkus-virtual-thread-")
    Optional<String> namePrefix;

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then any pending tasks will be interrupted, and the shutdown process will continue
     */
    @ConfigItem(defaultValue = "1M")
    public Duration shutdownTimeout;

    /**
     * The frequency at which the status of the executor service should be checked during shutdown.
     * Setting this key to an empty value disables the shutdown check interval.
     */
    @ConfigItem(defaultValue = "5s")
    public Optional<Duration> shutdownCheckInterval;
}
