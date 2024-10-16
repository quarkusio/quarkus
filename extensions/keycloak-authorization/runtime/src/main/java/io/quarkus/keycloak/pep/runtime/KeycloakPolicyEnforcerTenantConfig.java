package io.quarkus.keycloak.pep.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.ScopeEnforcementMode;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface KeycloakPolicyEnforcerTenantConfig {

    /**
     * Adapters will make separate HTTP invocations to the Keycloak server to turn an access code into an access token.
     * This config option defines how many connections to the Keycloak server should be pooled
     */
    @WithDefault("20")
    int connectionPoolSize();

    /**
     * Policy enforcement configuration when using Keycloak Authorization Services
     */
    KeycloakConfigPolicyEnforcer policyEnforcer();

    @ConfigGroup
    interface KeycloakConfigPolicyEnforcer {
        /**
         * Specifies how policies are enforced.
         */
        @WithDefault("enforcing")
        EnforcementMode enforcementMode();

        /**
         * Specifies the paths to protect.
         */
        Map<String, PathConfig> paths();

        /**
         * Defines how the policy enforcer should track associations between paths in your application and resources defined in
         * Keycloak.
         * The cache is needed to avoid unnecessary requests to a Keycloak server by caching associations between paths and
         * protected resources
         */
        PathCacheConfig pathCache();

        /**
         * Specifies how the adapter should fetch the server for resources associated with paths in your application. If true,
         * the
         * policy
         * enforcer is going to fetch resources on-demand accordingly with the path being requested
         */
        @WithDefault("true")
        boolean lazyLoadPaths();

        /**
         * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make these
         * claims available to policies
         */
        ClaimInformationPointConfig claimInformationPoint();

        /**
         * Specifies how scopes should be mapped to HTTP methods. If set to true, the policy enforcer will use the HTTP method
         * from
         * the current request to check whether access should be granted
         */
        @WithDefault("false")
        boolean httpMethodAsScope();

        @ConfigGroup
        interface PathConfig {

            /**
             * The name of a resource on the server that is to be associated with a given path
             */
            Optional<String> name();

            /**
             * A URI relative to the application’s context path that should be protected by the policy enforcer
             */
            @Deprecated(since = "Quarkus 3.10") // use the 'paths' configuration property
            Optional<String> path();

            /**
             * HTTP request paths that should be protected by the policy enforcer
             */
            Optional<List<String>> paths();

            /**
             * The HTTP methods (for example, GET, POST, PATCH) to protect and how they are associated with the scopes for a
             * given
             * resource in the server
             */
            Map<String, MethodConfig> methods();

            /**
             * Specifies how policies are enforced
             */
            @WithDefault("enforcing")
            EnforcementMode enforcementMode();

            /**
             * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make
             * these
             * claims available to policies
             */
            ClaimInformationPointConfig claimInformationPoint();
        }

        @ConfigGroup
        interface MethodConfig {

            /**
             * The name of the HTTP method
             */
            String method();

            /**
             * An array of strings with the scopes associated with the method
             */
            List<String> scopes();

            /**
             * A string referencing the enforcement mode for the scopes associated with a method
             */
            @WithDefault("all")
            ScopeEnforcementMode scopesEnforcementMode();
        }

        @ConfigGroup
        interface PathCacheConfig {

            /**
             * Defines the limit of entries that should be kept in the cache
             */
            @WithDefault("1000")
            int maxEntries();

            /**
             * Defines the time in milliseconds when the entry should be expired
             */
            @WithDefault("30000")
            long lifespan();
        }

        @ConfigGroup
        interface ClaimInformationPointConfig {

            /**
             * Complex config.
             */
            @WithParentName
            Map<String, Map<String, Map<String, String>>> complexConfig();

            /**
             * Simple config.
             */
            @WithParentName
            Map<String, Map<String, String>> simpleConfig();
        }
    }

    /**
     * Creates {@link KeycloakPolicyEnforcerTenantConfig} builder populated with documented default values.
     *
     * @return KeycloakPolicyEnforcerTenantConfigBuilder builder
     */
    static KeycloakPolicyEnforcerTenantConfigBuilder builder() {
        var defaultTenantConfig = new SmallRyeConfigBuilder()
                .withMapping(KeycloakPolicyEnforcerConfig.class)
                .build()
                .getConfigMapping(KeycloakPolicyEnforcerConfig.class)
                .defaultTenant();
        return new KeycloakPolicyEnforcerTenantConfigBuilder(defaultTenantConfig);
    }

    /**
     * Creates {@link KeycloakPolicyEnforcerTenantConfig} builder populated with {@code tenantConfig} values.
     *
     * @param tenantConfig tenant config; must not be null
     *
     * @return KeycloakPolicyEnforcerTenantConfigBuilder builder
     */
    static KeycloakPolicyEnforcerTenantConfigBuilder builder(KeycloakPolicyEnforcerTenantConfig tenantConfig) {
        Objects.requireNonNull(tenantConfig);
        return new KeycloakPolicyEnforcerTenantConfigBuilder(tenantConfig);
    }
}
