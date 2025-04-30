package io.quarkus.virtual.threads;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.virtual-threads")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface VirtualThreadsConfig {

    /**
     * Virtual thread name prefix. The name of the virtual thread will be the prefix followed by a unique number.
     */
    @WithDefault("quarkus-virtual-thread-")
    Optional<String> namePrefix();

    /**
     * The shutdown timeout. If all pending work has not been completed by this time
     * then any pending tasks will be interrupted, and the shutdown process will continue
     */
    @WithDefault("1M")
    Duration shutdownTimeout();

    /**
     * The frequency at which the status of the executor service should be checked during shutdown.
     * Setting this key to an empty value will use the same value as the shutdown timeout.
     */
    @WithDefault("5s")
    Optional<Duration> shutdownCheckInterval();

    /**
     * A flag to explicitly disabled virtual threads, even if the JVM support them.
     * In this case, methods annotated with {@code @RunOnVirtualThread} are executed on the worker thread pool.
     * <p>
     * This flag is intended to be used when running with virtual threads become more expensive than plain worker threads,
     * because of pinning, monopolization or thread-based object pool.
     */
    @WithDefault("true")
    boolean enabled();
}
