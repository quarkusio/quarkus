package io.quarkus.observation.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.observation")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ObservationBuildConfig {

    /**
     * Whether the Observation API support is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
