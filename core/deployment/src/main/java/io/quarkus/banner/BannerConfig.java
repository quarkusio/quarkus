package io.quarkus.banner;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Banner
 */
@ConfigMapping(prefix = "quarkus.banner")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface BannerConfig {
    String DEFAULT_BANNER_FILE = "default_banner.txt";

    /**
     * The path of the banner (path relative to root of classpath)
     * which could be provided by user
     */
    @WithDefault(DEFAULT_BANNER_FILE)
    String path();

    default boolean isDefaultPath() {
        return DEFAULT_BANNER_FILE.equals(path());
    }
}
