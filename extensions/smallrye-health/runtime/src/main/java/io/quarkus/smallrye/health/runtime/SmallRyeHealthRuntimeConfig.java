package io.quarkus.smallrye.health.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-health", phase = ConfigPhase.RUN_TIME)
public class SmallRyeHealthRuntimeConfig {

    /**
     * If Health UI should be enabled. By default, Health UI is enabled if it is included (see {@code always-include}).
     */
    @ConfigItem(name = "ui.enable", defaultValue = "true")
    boolean enable;

    /**
     * Additional top-level properties to be included in the resulting JSON object.
     */
    @ConfigItem(name = "additional.property")
    @ConfigDocMapKey("property-name")
    Map<String, String> additionalProperties;

    /**
     * Specifications of checks that can be disabled.
     */
    @ConfigItem
    @ConfigDocMapKey("check-name")
    Map<String, Enabled> check;

    @ConfigGroup
    public static final class Enabled {

        /**
         * Whether the HealthCheck should be enabled.
         */
        @ConfigItem
        boolean enabled;
    }
}
