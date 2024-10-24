package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Build time analytics.
 * <p>
 * This is a dummy config class to hide the warnings on the comment line.
 * All properties in here are actually used in the build tools.
 */
@ConfigMapping(prefix = "quarkus.analytics")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface BuildAnalyticsConfig {

    /**
     * If Build time analytics are disabled.
     */
    Optional<Boolean> disabled();

    /**
     * The Segment base URI.
     */
    @WithName("uri.base")
    Optional<String> uriBase();

    /**
     * The Timeout to send the build time analytics to segment.
     */
    @WithDefault("3000")
    Integer timeout();
}
