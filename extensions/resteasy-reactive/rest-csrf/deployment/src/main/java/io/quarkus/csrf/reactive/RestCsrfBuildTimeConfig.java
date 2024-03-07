package io.quarkus.csrf.reactive;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for CSRF Reactive Filter.
 */
@ConfigRoot
public class RestCsrfBuildTimeConfig {
    /**
     * If filter is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
