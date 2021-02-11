package io.quarkus.smallrye.health.runtime;

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
}
