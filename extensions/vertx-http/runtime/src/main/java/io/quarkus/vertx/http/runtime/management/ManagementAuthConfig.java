package io.quarkus.vertx.http.runtime.management;

import java.util.Map;
import java.util.Optional;

import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Authentication for the management interface.
 */
public interface ManagementAuthConfig {
    /**
     * If basic auth should be enabled.
     *
     */
    Optional<Boolean> basic();

    /**
     * The HTTP permissions
     */
    @WithName("permission")
    Map<String, PolicyMappingConfig> permissions();

    /**
     * The HTTP role based policies
     */
    @WithName("policy")
    Map<String, PolicyConfig> rolePolicy();

    /**
     * If this is true and credentials are present then a user will always be authenticated
     * before the request progresses.
     *
     * If this is false then an attempt will only be made to authenticate the user if a permission
     * check is performed or the current user is required for some other reason.
     */
    @WithDefault("true")
    boolean proactive();
}
