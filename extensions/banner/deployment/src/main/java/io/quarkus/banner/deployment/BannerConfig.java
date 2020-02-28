package io.quarkus.banner.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "banner")
public class BannerConfig {

    /**
     * Whether or not the banner will be displayed
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The path of the banner (path relative to root of classpath)
     * which could be provided by user
     */
    @ConfigItem(defaultValue = "default_banner.txt")
    public String path;
}
