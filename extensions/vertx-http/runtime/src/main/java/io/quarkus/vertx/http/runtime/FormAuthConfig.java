package io.quarkus.vertx.http.runtime;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * config for the form authentication mechanism
 */
public interface FormAuthConfig {
    /**
     * If form authentication is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * If one-time authentication token support is enabled.
     */
    @WithDefault("false")
    @WithName("authentication-token.enabled")
    boolean authenticationTokenEnabled();

}
