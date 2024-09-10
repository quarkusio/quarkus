package io.quarkus.banner;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Banner
 */
@ConfigRoot(name = "banner")
public class BannerConfig {

    private static final String DEFAULT_BANNER_FILE = "default_banner.txt";

    /**
     * The path of the banner (path relative to root of classpath)
     * which could be provided by user
     */
    @ConfigItem(defaultValue = DEFAULT_BANNER_FILE)
    public String path;

    public boolean isDefaultPath() {
        return DEFAULT_BANNER_FILE.equals(path);
    }
}
