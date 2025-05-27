package io.quarkus.vertx.http.runtime;

import io.smallrye.config.WithDefault;

/**
 * config for the form authentication mechanism
 */
public interface FormAuthConfig {
    /**
     * If form authentication is enabled.
     */
    @WithDefault("false")
    boolean enabled();

}
