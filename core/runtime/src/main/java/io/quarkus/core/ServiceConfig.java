package io.quarkus.core;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Service lifecycle configuration.
 */
@ConfigMapping(prefix = "quarkus.service")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ServiceConfig {

    /**
     * Whether to start and stop services in parallel using the thread pool executor.
     * When disabled, all services start and stop sequentially on the main thread.
     */
    @WithDefault("true")
    boolean parallel();
}
