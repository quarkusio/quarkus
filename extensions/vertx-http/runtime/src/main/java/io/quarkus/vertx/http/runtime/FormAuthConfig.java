package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * config for the form authentication mechanism
 */
@ConfigGroup
public interface FormAuthConfig {

    /**
     * If form authentication is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The post location.
     */
    @WithDefault("/j_security_check")
    String postLocation();

}
