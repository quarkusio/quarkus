package io.quarkus.keycloak.pep.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class KeycloakPolicyEnforcerTenantConfig {

    /**
     * Adapters will make separate HTTP invocations to the Keycloak server to turn an access code into an access token.
     * This config option defines how many connections to the Keycloak server should be pooled
     */
    @ConfigItem(defaultValue = "20")
    int connectionPoolSize;

    /**
     * Policy enforcement configuration when using Keycloak Authorization Services
     */
    @ConfigItem
    public KeycloakConfigPolicyEnforcer policyEnforcer = new KeycloakConfigPolicyEnforcer();

    @ConfigGroup
    public static class KeycloakConfigPolicyEnforcer {
        /**
         * Specifies how policies are enforced.
         */
        @ConfigItem(defaultValue = "enforcing")
        public PolicyEnforcerConfig.EnforcementMode enforcementMode;

        /**
         * Specifies the paths to protect.
         */
        @ConfigItem
        public Map<String, PathConfig> paths;

        /**
         * Defines how the policy enforcer should track associations between paths in your application and resources defined in
         * Keycloak.
         * The cache is needed to avoid unnecessary requests to a Keycloak server by caching associations between paths and
         * protected resources
         */
        @ConfigItem
        public PathCacheConfig pathCache = new PathCacheConfig();

        /**
         * Specifies how the adapter should fetch the server for resources associated with paths in your application. If true,
         * the
         * policy
         * enforcer is going to fetch resources on-demand accordingly with the path being requested
         */
        @ConfigItem(defaultValue = "true")
        public boolean lazyLoadPaths;

        /**
         * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make these
         * claims available to policies
         */
        @ConfigItem
        public ClaimInformationPointConfig claimInformationPoint;

        /**
         * Specifies how scopes should be mapped to HTTP methods. If set to true, the policy enforcer will use the HTTP method
         * from
         * the current request to check whether or not access should be granted
         */
        @ConfigItem
        public boolean httpMethodAsScope;

        @ConfigGroup
        public static class PathConfig {

            /**
             * The name of a resource on the server that is to be associated with a given path
             */
            @ConfigItem
            public Optional<String> name;

            /**
             * A URI relative to the application’s context path that should be protected by the policy enforcer
             */
            @ConfigItem
            public Optional<String> path;

            /**
             * The HTTP methods (for example, GET, POST, PATCH) to protect and how they are associated with the scopes for a
             * given
             * resource in the server
             */
            @ConfigItem
            public Map<String, MethodConfig> methods;

            /**
             * Specifies how policies are enforced
             */
            @ConfigItem(defaultValue = "enforcing")
            public PolicyEnforcerConfig.EnforcementMode enforcementMode;

            /**
             * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make
             * these
             * claims available to policies
             */
            @ConfigItem
            public ClaimInformationPointConfig claimInformationPoint;
        }

        @ConfigGroup
        public static class MethodConfig {

            /**
             * The name of the HTTP method
             */
            @ConfigItem
            public String method;

            /**
             * An array of strings with the scopes associated with the method
             */
            @ConfigItem
            public List<String> scopes;

            /**
             * A string referencing the enforcement mode for the scopes associated with a method
             */
            @ConfigItem(defaultValue = "all")
            public PolicyEnforcerConfig.ScopeEnforcementMode scopesEnforcementMode;
        }

        @ConfigGroup
        public static class PathCacheConfig {

            /**
             * Defines the limit of entries that should be kept in the cache
             */
            @ConfigItem(defaultValue = "1000")
            public int maxEntries = 1000;

            /**
             * Defines the time in milliseconds when the entry should be expired
             */
            @ConfigItem(defaultValue = "30000")
            public long lifespan = 30000;
        }

        @ConfigGroup
        public static class ClaimInformationPointConfig {

            /**
             *
             */
            @ConfigItem(name = ConfigItem.PARENT)
            public Map<String, Map<String, Map<String, String>>> complexConfig;

            /**
             *
             */
            @ConfigItem(name = ConfigItem.PARENT)
            public Map<String, Map<String, String>> simpleConfig;
        }
    }
}
