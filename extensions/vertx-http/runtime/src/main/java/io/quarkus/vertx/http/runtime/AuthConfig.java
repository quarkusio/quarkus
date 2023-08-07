package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Optional;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Authentication mechanism and SecurityRealm name information used for configuring HTTP auth
 * instance for the deployment.
 */
public interface AuthConfig {
    /**
     * If basic auth should be enabled. If both basic and form auth is enabled then basic auth will be enabled in silent mode.
     *
     * If no authentication mechanisms are configured basic auth is the default.
     */
    Optional<Boolean> basic();

    /**
     * Form Auth config
     */
    FormAuthConfig form();

    /**
     * The authentication realm
     */
    Optional<String> realm();

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
