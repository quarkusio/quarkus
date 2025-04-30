package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Banner
 */
@ConfigMapping(prefix = "quarkus.banner")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface BannerRuntimeConfig {

    /**
     * Whether the banner will be displayed
     */
    @WithDefault("true")
    boolean enabled();
}
