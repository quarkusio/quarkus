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
     * Require that all registered HTTP authentication mechanisms must complete the authentication.
     * <p>
     * Typically, this property has to be true when the credentials are carried over mTLS, when both mTLS and another
     * authentication, for example, OIDC bearer token authentication, must succeed.
     * In such cases, `SecurityIdentity` created by the first mechanism, mTLS, can be injected, identities created
     * by other mechanisms will be available on `SecurityIdentity`.
     * The mTLS mechanism is always the first mechanism, because its priority is elevated when inclusive authentication
     * is enabled.
     * The identities can be retrieved using utility method as in the example below:
     *
     * <pre>
     * {@code
     * io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getSecurityIdentities(securityIdentity)
     * }
     * </pre>
     * <p>
     * This property is false by default which means that the authentication process is complete as soon as the first
     * `SecurityIdentity` is created.
     * <p>
     * This property will be ignored if the path specific authentication is enabled.
     */
    @WithDefault("false")
    boolean inclusive();
}
