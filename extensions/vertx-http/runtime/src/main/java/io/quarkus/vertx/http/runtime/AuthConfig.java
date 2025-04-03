package io.quarkus.vertx.http.runtime;

import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * Authentication mechanism and SecurityRealm name information used for configuring HTTP auth
 * instance for the deployment.
 */
public interface AuthConfig {
    /**
     * If basic auth should be enabled. If both basic and form auth is enabled then basic auth will be enabled in silent mode.
     * <p>
     * The basic auth is enabled by default if no authentication mechanisms are configured or Quarkus can safely
     * determine that basic authentication is required.
     */
    Optional<Boolean> basic();

    /**
     * Form Auth config
     */
    FormAuthConfig form();

    /**
     * If this is true and credentials are present then a user will always be authenticated
     * before the request progresses.
     * <p>
     * If this is false then an attempt will only be made to authenticate the user if a permission
     * check is performed or the current user is required for some other reason.
     */
    @WithDefault("true")
    boolean proactive();

    /**
     * Propagate security identity to support its injection in Vert.x route handlers registered directly with the router.
     */
    @WithDefault("false")
    boolean propagateSecurityIdentity();

}
