package io.quarkus.smallrye.health.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.smallrye-health")
public interface SmallRyeHealthRuntimeConfig {

    /**
     * If Health UI should be enabled. By default, Health UI is enabled if it is included (see {@code always-include}).
     */
    @WithName("ui.enable")
    @WithDefault("true")
    boolean enable();

    /**
     * Additional top-level properties to be included in the resulting JSON object.
     */
    @WithName("additional.property")
    @ConfigDocMapKey("property-name")
    Map<String, String> additionalProperties();

    /**
     * Specifications of checks that can be disabled.
     */
    @ConfigDocMapKey("check-classname")
    Map<String, Enabled> check();

    @ConfigGroup
    interface Enabled {

        /**
         * Whether the HealthCheck should be enabled.
         */
        boolean enabled();
    }
}
