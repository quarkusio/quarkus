package io.quarkus.banner.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "banner", phase = ConfigPhase.BUILD_TIME)
public class BannerConfig {
    /**
     * The path of the banner which could be provided by user
     */
    @ConfigItem(defaultValue = "default_banner.txt")
    public String path;
}
