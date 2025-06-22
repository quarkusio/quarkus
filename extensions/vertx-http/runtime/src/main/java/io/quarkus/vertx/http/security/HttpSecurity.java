package io.quarkus.vertx.http.security;

import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.vertx.ext.web.RoutingContext;

/**
 * A CDI event that facilitates programmatic path-specific authorization setup.
 * The event can be observed with synchronous observer method like in the example below:
 *
 * <pre>
 * {@code
 * import jakarta.enterprise.event.Observes;
 *
 * public class HttpSecurityConfiguration {
 *
 *     void observe(@Observes HttpSecurity httpSecurity) {
 *         httpSecurity
 *                 .path("/admin/*").basic().roles("admin")
 *                 .path("/user/*").form().roles("user")
 *                 .path("/public/*").permit();
 *         // and:
 *         httpSecurity.path("/root*").authorization()
 *                 .policy(identity -> "root".equals(identity.getPrincipal().getName()));
 *     }
 * }
 * }
 * </pre>
 *
 * If multiple path-patterns matches an incoming request path, the most specific pattern wins.
 * Expected behavior for the programmatic configuration is very much same as for the HTTP permissions
 * specified in the 'application.properties' file.
 * For example following configuration properties:
 *
 * <pre>
 * {@code
 * quarkus.http.auth.permission.deny1.paths=/forbidden
 * quarkus.http.auth.permission.deny1.policy=deny
 * }
 * </pre>
 *
 * can be also written as:
 *
 * <pre>
 * {@code
 * httpSecurity.path("/forbidden").authorization().deny();
 * }
 * </pre>
 *
 * Programmatic setup for the management interface is currently not supported.
 * This CDI event is fired when the runtime configuration is ready,
 * therefore you can inject configuration properties like this:
 *
 * <pre>
 * {@code
 * import jakarta.enterprise.event.Observes;
 *
 * import io.quarkus.vertx.http.security.HttpSecurity;
 * import org.eclipse.microprofile.config.inject.ConfigProperty;
 *
 * public class HttpSecurityConfiguration {
 *
 *     void configure(@Observes HttpSecurity httpSecurity, @ConfigProperty(name = "admin1-role") String admin1) {
 *         httpSecurity.rolesMapping("admin", admin1);
 *     }
 * }
 * }
 * </pre>
 */
public interface HttpSecurity {

    /**
     * Creates {@link HttpPermission}.
     *
     * @param paths path patterns; this is programmatic analogy to the 'quarkus.http.auth.permission."permissions".paths'
     *        configuration property, same rules apply
     * @return new {@link HttpPermission}
     */
    HttpPermission path(String... paths);

    /**
     * This method is a shortcut for {@code path(path).methods("GET")}.
     *
     * @see #path(String...)
     */
    HttpPermission get(String... paths);

    /**
     * This method is a shortcut for {@code path(path).methods("PUT")}.
     *
     * @see #path(String...)
     */
    HttpPermission put(String... paths);

    /**
     * This method is a shortcut for {@code path(path).methods("POST")}.
     *
     * @see #path(String...)
     */
    HttpPermission post(String... paths);

    /**
     * This method is a shortcut for {@code path(path).methods("DELETE")}.
     *
     * @see #path(String...)
     */
    HttpPermission delete(String... paths);

    /**
     * Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.
     * Programmatic analogy to the 'quarkus.http.auth.roles-mapping."role-name"' configuration property.
     * If the configuration property is already set, invocation of this method fails as both methods are mutually exclusive.
     */
    HttpSecurity rolesMapping(Map<String, List<String>> roleToRoles);

    /**
     * @see #rolesMapping(Map)
     */
    HttpSecurity rolesMapping(String sourceRole, List<String> targetRoles);

    /**
     * @see #rolesMapping(Map)
     */
    HttpSecurity rolesMapping(String sourceRole, String targetRole);

    /**
     * Represents authorization and authentication requirements for given path patterns.
     */
    interface HttpPermission {

        /**
         * HTTP request must be authenticated using basic authentication.
         */
        HttpPermission basic();

        /**
         * HTTP request must be authenticated using form-based authentication.
         */
        HttpPermission form();

        /**
         * HTTP request must be authenticated using mutual-TLS authentication.
         */
        HttpPermission mTLS();

        /**
         * HTTP request must be authenticated using Bearer token authentication.
         */
        HttpPermission bearer();

        /**
         * HTTP request must be authenticated using WebAuthn mechanism.
         */
        HttpPermission webAuthn();

        /**
         * HTTP request must be authenticated using Authorization Code Flow mechanism.
         */
        HttpPermission authorizationCodeFlow();

        /**
         * HTTP requests will only be accessible if {@link io.quarkus.security.identity.SecurityIdentity}
         * is not anonymous.
         */
        HttpSecurity authenticated();

        /**
         * HTTP request must be authenticated using a mechanism
         * with matching {@link HttpCredentialTransport#getAuthenticationScheme()}.
         * Please note that annotation-based mechanism selection has higher priority during the mechanism selection.
         */
        HttpPermission authenticatedWith(String scheme);

        /**
         * HTTP request must be authenticated with this mechanism.
         * Please note that annotation-based mechanism selection has higher priority during the mechanism selection.
         */
        HttpPermission authenticatedWith(HttpAuthenticationMechanism mechanism);

        /**
         * Indicates that this policy always applies to the matched paths in addition to the policy with a winning path.
         * Programmatic analogy to the 'quarkus.http.auth.permission."permissions".shared' configuration property.
         */
        HttpPermission shared();

        /**
         * Whether permission check should be applied on all matching paths, or paths specific for the Jakarta REST resources.
         * Programmatic analogy to the 'quarkus.http.auth.permission."permissions".applies-to' configuration property.
         */
        HttpPermission applyToJaxRs();

        /**
         * The methods that this permission set applies to. If this is not set then they apply to all methods.
         * Programmatic analogy to the 'quarkus.http.auth.permission."permissions".methods' configuration property.
         */
        HttpPermission methods(String... httpMethods);

        /**
         * Allows to configure HTTP request authorization requirement on the returned instance.
         */
        Authorization authorization();

        /**
         * This method is a shortcut for {@link Authorization#permit()}.
         */
        HttpSecurity permit();

        /**
         * This method is a shortcut for {@link Authorization#roles(String...)}.
         */
        HttpSecurity roles(String... roles);

        /**
         * This method is a shortcut for {@link Authorization#policy(HttpSecurityPolicy)}.
         */
        HttpSecurity policy(HttpSecurityPolicy httpSecurityPolicy);
    }

    /**
     * Represents HTTP request authorization.
     */
    interface Authorization {

        /**
         * Access to HTTP requests will be permitted.
         */
        HttpSecurity permit();

        /**
         * Access to HTTP requests will be denied.
         */
        HttpSecurity deny();

        /**
         * HTTP requests will only be accessible if {@link io.quarkus.security.identity.SecurityIdentity}
         * has all the required roles. Roles must be literal, property expansion is not supported here.
         *
         * @param roleToRoles see the 'quarkus.http.auth.policy."role-policy".roles."role-name"' configuration property
         * @param rolesAllowed see the 'quarkus.http.auth.policy."role-policy".roles-allowed' configuration property
         * @return HttpSecurity
         */
        HttpSecurity roles(Map<String, List<String>> roleToRoles, String... rolesAllowed);

        /**
         * HTTP requests will only be accessible if {@link io.quarkus.security.identity.SecurityIdentity}
         * has all the required roles. Roles must be literal, property expansion is not supported here.
         */
        HttpSecurity roles(String... roles);

        /**
         * HTTP requests will only be accessible if {@link io.quarkus.security.identity.SecurityIdentity}
         * has all the required permissions.
         */
        HttpSecurity permissions(Permission... requiredPermissions);

        /**
         * HTTP requests will only be accessible if {@link io.quarkus.security.identity.SecurityIdentity}
         * has all the required {@link io.quarkus.security.StringPermission}s.
         *
         * @param permissionNames required {@link Permission#getName()}
         */
        HttpSecurity permissions(String... permissionNames);

        /**
         * HTTP requests will only be accessible if the passed {@link HttpSecurityPolicy} grants access.
         */
        HttpSecurity policy(HttpSecurityPolicy policy);

        /**
         * HTTP requests will only be accessible if the passed predicate returns {@code true}.
         * This is a shortcut method for {@link #policy(HttpSecurityPolicy)}.
         * The {@link SecurityIdentity} in this special case is never anonymous, anonymous requests will be denied.
         */
        HttpSecurity policy(Predicate<SecurityIdentity> predicate);

        /**
         * HTTP requests will only be accessible if the passed predicate returns {@code true}.
         * This is a shortcut method for {@link #policy(HttpSecurityPolicy)}.
         */
        HttpSecurity policy(BiPredicate<SecurityIdentity, RoutingContext> predicate);
    }
}
