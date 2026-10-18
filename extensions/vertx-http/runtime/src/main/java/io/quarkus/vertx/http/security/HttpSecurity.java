package io.quarkus.vertx.http.security;

import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.smallrye.common.annotation.Experimental;
import io.vertx.core.http.ClientAuth;
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
@Experimental("This API is currently experimental and might get changed")
public interface HttpSecurity {

    /**
     * Creates a new CORS configuration with given origin.
     * This method is a shortcut for {@code cors(Set.of(origin))}.
     *
     * @param origin see {@link CORSConfig#origins()}
     * @return HttpSecurity
     */
    HttpSecurity cors(String origin);

    /**
     * Creates a new CORS configuration with given origins.
     * This method is a shortcut for {@code cors(CORS.origins(origins).build())}.
     *
     * @param origins see {@link CORSConfig#origins()}
     * @return HttpSecurity
     */
    HttpSecurity cors(Set<String> origins);

    /**
     * Enables the CORS filter with given configuration.
     *
     * @param cors {@link CORS} filter configuration
     * @return HttpSecurity
     */
    HttpSecurity cors(CORS cors);

    /**
     * Configures the Cross-Site Request Forgery (CSRF) prevention.
     *
     * @param csrf {@link CSRF} prevention configuration
     * @return HttpSecurity
     */
    HttpSecurity csrf(CSRF csrf);

    /**
     * Registers given {@link HttpAuthenticationMechanism} in addition to all other global authentication mechanisms.
     *
     * @param mechanism {@link HttpAuthenticationMechanism}
     * @return HttpSecurity
     */
    HttpSecurity mechanism(HttpAuthenticationMechanism mechanism);

    /**
     * Registers the Basic authentication mechanism in addition to all other global authentication mechanisms.
     * This method is a shortcut for {@code mechanism(Basic.create())}.
     *
     * @return HttpSecurity
     */
    HttpSecurity basic();

    /**
     * Registers the Basic authentication mechanism in addition to all other global authentication mechanisms.
     * This method is a shortcut for {@code mechanism(Basic.realm(authenticationRealm))}.
     *
     * @param authenticationRealm see the 'quarkus.http.auth.realm' configuration property
     * @return HttpSecurity
     */
    HttpSecurity basic(String authenticationRealm);

    /**
     * Registers the mutual TLS client authentication mechanism in addition to all other global authentication mechanisms.
     * This method is a shortcut for {@code mTLS(ClientAuth.REQUIRED)}, therefore the client authentication is required.
     *
     * @return HttpSecurity
     * @see #mTLS(ClientAuth) for more information
     */
    HttpSecurity mTLS();

    /**
     * Registers the mutual TLS client authentication mechanism in addition to all other global authentication mechanisms.
     * The TLS configuration is registered against the registry and is used by the HTTP server for the TLS communication.
     * This method is a shortcut for the {@code httpSecurity.mTLS(MTLS.required(tlsConfigurationName, tlsConfiguration))},
     * therefore the client authentication is required.
     *
     * @param tlsConfigurationName the name of the configuration, cannot be {@code null}, cannot be {@code <default>}
     * @param tlsConfiguration the configuration cannot be {@code null}
     * @return HttpSecurity
     * @see VertxHttpBuildTimeConfig#tlsClientAuth() for more information about {@link ClientAuth#REQUEST}
     * @see MTLS.Builder#tls(String, TlsConfiguration) for information about method parameters and TLS config registration
     */
    HttpSecurity mTLS(String tlsConfigurationName, TlsConfiguration tlsConfiguration);

    /**
     * Registers the mutual TLS client authentication mechanism in addition to all other global authentication mechanisms.
     *
     * @param mTLSAuthenticationMechanism {@link MtlsAuthenticationMechanism} build with the {@link MTLS} API
     * @return HttpSecurity
     */
    HttpSecurity mTLS(MtlsAuthenticationMechanism mTLSAuthenticationMechanism);

    /**
     * Registers the mutual TLS client authentication mechanism in addition to all other global authentication mechanisms.
     * If you need to define the client certificate attribute value to role mappings, please use the {@link MTLS} builder.
     *
     * @param tlsClientAuth either {@link ClientAuth#REQUEST} or {@link ClientAuth#REQUIRED}; for more information,
     *        see the {@link VertxHttpBuildTimeConfig#tlsClientAuth()} configuration property
     * @return HttpSecurity
     */
    HttpSecurity mTLS(ClientAuth tlsClientAuth);

    /**
     * Creates {@link HttpPermission} in addition to the permissions configured in the 'application.properties' file.
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
         * HTTP request must be authenticated using basic authentication mechanism configured
         * in the 'application.properties' file or the mechanism created with the {@link Basic} API and registered
         * against the {@link HttpSecurity#mechanism(HttpAuthenticationMechanism)}.
         */
        HttpPermission basic();

        /**
         * HTTP request must be authenticated using form-based authentication mechanism configured
         * in the 'application.properties' file or the mechanism created with the {@link Form} API and registered
         * against the {@link HttpSecurity#mechanism(HttpAuthenticationMechanism)}.
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
         */
        HttpPermission authenticatedWith(String scheme);

        /**
         * HTTP request must be authenticated using mechanisms with matching
         * {@link HttpCredentialTransport#getAuthenticationScheme()}. By default, only one of the matching authentication
         * mechanisms will produce a {@link SecurityIdentity}, and all matching authentication mechanisms will attempt
         * to authenticate when an inclusive authentication is enabled.
         */
        HttpPermission authenticatedWith(Set<String> schemes);

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
