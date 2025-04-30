package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface PolicyMappingConfig {
    /**
     * Determines whether the entire permission set is enabled, or not.
     * <p>
     * By default, if the permission set is defined, it is enabled.
     */
    Optional<Boolean> enabled();

    /**
     * The HTTP policy that this permission set is linked to.
     * <p>
     * There are three built-in policies: permit, deny and authenticated. Role based
     * policies can be defined, and extensions can add their own policies.
     */
    String policy();

    /**
     * The methods that this permission set applies to. If this is not set then they apply to all methods.
     * <p>
     * Note that if a request matches any path from any permission set, but does not match the constraint
     * due to the method not being listed then the request will be denied.
     * <p>
     * Method specific permissions take precedence over matches that do not have any methods set.
     * <p>
     * This means that for example if Quarkus is configured to allow GET and POST requests to /admin to
     * and no other permissions are configured PUT requests to /admin will be denied.
     *
     */
    Optional<List<String>> methods();

    /**
     * The paths that this permission check applies to. If the path ends in /* then this is treated
     * as a path prefix, otherwise it is treated as an exact match.
     * <p>
     * Matches are done on a length basis, so the most specific path match takes precedence.
     * <p>
     * If multiple permission sets match the same path then explicit methods matches take precedence
     * over matches without methods set, otherwise the most restrictive permissions are applied.
     *
     */
    Optional<List<String>> paths();

    /**
     * Path specific authentication mechanism which must be used to authenticate a user.
     * It needs to match {@link io.quarkus.vertx.http.runtime.security.HttpCredentialTransport} authentication scheme
     * such as 'basic', 'bearer', 'form', etc.
     */
    Optional<String> authMechanism();

    /**
     * Indicates that this policy always applies to the matched paths in addition to the policy with a winning path.
     * Avoid creating more than one shared policy to minimize the performance impact.
     */
    @WithDefault("false")
    boolean shared();

    /**
     * Whether permission check should be applied on all matching paths, or paths specific for the Jakarta REST resources.
     */
    @WithDefault("ALL")
    AppliesTo appliesTo();

    /**
     * Specifies additional criteria on paths that should be checked.
     */
    enum AppliesTo {
        /**
         * Apply on all matching paths.
         */
        ALL,
        /**
         * Declares that a permission check must only be applied on the Jakarta REST request paths.
         * Use this option to delay the permission check if an authentication mechanism is chosen with an annotation on
         * the matching Jakarta REST endpoint. This option must be set if the following REST endpoint annotations are used:
         * <ul>
         * <li>`io.quarkus.oidc.Tenant` annotation which selects an OIDC authentication mechanism with a tenant identifier</li>
         * <li>`io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication` which selects the Basic authentication
         * mechanism</li>
         * <li>`io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication` which selects the Form-based
         * authentication mechanism</li>
         * <li>`io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication` which selects the mTLS authentication
         * mechanism</li>
         * <li>`io.quarkus.security.webauthn.WebAuthn` which selects the WebAuth authentication mechanism</li>
         * <li>`io.quarkus.oidc.BearerTokenAuthentication` which selects the OpenID Connect Bearer token authentication
         * mechanism</li>
         * <li>`io.quarkus.oidc.AuthorizationCodeFlow` which selects the OpenID Connect Code authentication mechanism</li>
         * </ul>
         */
        JAXRS
    }
}
