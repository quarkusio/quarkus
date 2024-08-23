package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.messaging")
public interface HealthCenterFilterConfig {

    /**
     * Configuration for the health center filter.
     */
    @ConfigDocMapKey("channel")
    @ConfigDocSection
    Map<String, HealthCenterConfig> health();

    @ConfigGroup
    interface HealthCenterConfig {

        /**
         * Whether all health check is enabled
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Whether the readiness health check is enabled.
         */
        @WithDefault("true")
        @WithName("readiness.enabled")
        boolean readinessEnabled();

        /**
         * Whether the liveness health check is enabled.
         */
        @WithDefault("true")
        @WithName("liveness.enabled")
        boolean livenessEnabled();

        /**
         * Whether the startup health check is enabled.
         */
        @WithDefault("true")
        @WithName("startup.enabled")
        boolean startupEnabled();
    }

}
