package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class BannerRuntimeConfig {

    /**
     * Whether the banner will be displayed
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Display the tip of the day, next to the banner
     */
    @ConfigItem(defaultValue = "true")
    public boolean tips;
}
