package io.quarkus.csrf.reactive.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for CSRF Reactive Filter.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.rest-csrf")
public interface RestCsrfBuildTimeConfig {
    /**
     * If filter is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
