package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time analytics.
 * <p>
 * This is a dummy config class to hide the warnings on the comment line.
 * All properties in here are actually used in the build tools.
 */
@ConfigRoot(name = "analytics", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class BuildAnalyticsConfig {

    /**
     * If Build time analytics are disabled.
     */
    @ConfigItem
    public Optional<Boolean> disabled;

    /**
     * The Segment base URI.
     */
    @ConfigItem(name = "uri.base")
    public Optional<String> uriBase;

    /**
     * The Timeout to send the build time analytics to segment.
     */
    @ConfigItem(defaultValue = "3000")
    public Integer timeout;
}
