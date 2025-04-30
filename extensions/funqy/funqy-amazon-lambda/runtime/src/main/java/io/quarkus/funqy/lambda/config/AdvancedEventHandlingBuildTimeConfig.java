package io.quarkus.funqy.lambda.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Advanced event handling build time configuration
 */
@ConfigGroup
public interface AdvancedEventHandlingBuildTimeConfig {

    /**
     * If advanced event handling should be enabled
     */
    @WithDefault("true")
    boolean enabled();
}
