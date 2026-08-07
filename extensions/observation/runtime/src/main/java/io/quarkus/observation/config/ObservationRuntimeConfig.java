package io.quarkus.observation.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.observation")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ObservationRuntimeConfig {

    /**
     * Whether to register a handler that prints observation lifecycle events to the log.
     * Useful for debugging. The handler is registered last, after all other handlers.
     */
    @WithDefault("false")
    boolean printOut();
}
