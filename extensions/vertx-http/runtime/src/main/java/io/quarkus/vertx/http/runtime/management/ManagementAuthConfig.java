package io.quarkus.vertx.http.runtime.management;

import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * Authentication for the management interface.
 */
public interface ManagementAuthConfig {
    /**
     * If authentication for the management interface should be enabled.
     */
    @WithDefault("${quarkus.management.auth.basic:false}")
    boolean enabled();

    /**
     * If basic auth should be enabled.
     */
    Optional<Boolean> basic();

    /**
     * If this is true and credentials are present then a user will always be authenticated
     * before the request progresses.
     * <p>
     * If this is false then an attempt will only be made to authenticate the user if a permission
     * check is performed or the current user is required for some other reason.
     */
    @WithDefault("true")
    boolean proactive();
}
