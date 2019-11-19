package io.quarkus.keycloak.pep.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.DefaultConverter;

@ConfigRoot(name = "keycloak", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class KeycloakPolicyEnforcerConfig {

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
    public KeycloakConfigPolicyEnforcer policyEnforcer;

    @ConfigGroup
    public static class KeycloakConfigPolicyEnforcer {

        /**
         * Enables policy enforcement.
         */
        @ConfigItem
        public boolean enable;

        /**
         * Specifies how policies are enforced.
         */
        @ConfigItem(defaultValue = "ENFORCING")
        String enforcementMode;

        /**
         * Specifies the paths to protect.
         */
        @ConfigItem
        Map<String, PathConfig> paths;

        /**
         * Defines how the policy enforcer should track associations between paths in your application and resources defined in
         * Keycloak.
         * The cache is needed to avoid unnecessary requests to a Keycloak server by caching associations between paths and
         * protected resources
         */
        @ConfigItem
        PathCacheConfig pathCache;

        /**
         * Specifies how the adapter should fetch the server for resources associated with paths in your application. If true,
         * the
         * policy
         * enforcer is going to fetch resources on-demand accordingly with the path being requested
         */
        @ConfigItem(defaultValue = "true")
        boolean lazyLoadPaths;

        /**
         * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make these
         * claims available to policies
         */
        @ConfigItem
        ClaimInformationPointConfig claimInformationPoint;

        /**
         * Specifies how scopes should be mapped to HTTP methods. If set to true, the policy enforcer will use the HTTP method
         * from
         * the current request to check whether or not access should be granted
         */
        @ConfigItem
        boolean httpMethodAsScope;

        @ConfigGroup
        public static class PathConfig {

            /**
             * The name of a resource on the server that is to be associated with a given path
             */
            @ConfigItem
            Optional<String> name;

            /**
             * A URI relative to the application’s context path that should be protected by the policy enforcer
             */
            @ConfigItem
            Optional<String> path;

            /**
             * The HTTP methods (for example, GET, POST, PATCH) to protect and how they are associated with the scopes for a
             * given
             * resource in the server
             */
            @ConfigItem
            Map<String, MethodConfig> methods;

            /**
             * Specifies how policies are enforced
             */
            @DefaultConverter
            @ConfigItem(defaultValue = "ENFORCING")
            PolicyEnforcerConfig.EnforcementMode enforcementMode;

            /**
             * Defines a set of one or more claims that must be resolved and pushed to the Keycloak server in order to make
             * these
             * claims available to policies
             */
            @ConfigItem
            ClaimInformationPointConfig claimInformationPoint;
        }

        @ConfigGroup
        public static class MethodConfig {

            /**
             * The name of the HTTP method
             */
            @ConfigItem
            String method;

            /**
             * An array of strings with the scopes associated with the method
             */
            @ConfigItem
            List<String> scopes;

            /**
             * A string referencing the enforcement mode for the scopes associated with a method
             */
            @DefaultConverter
            @ConfigItem(defaultValue = "ALL")
            PolicyEnforcerConfig.ScopeEnforcementMode scopesEnforcementMode;
        }

        @ConfigGroup
        public static class PathCacheConfig {

            /**
             * Defines the time in milliseconds when the entry should be expired
             */
            @ConfigItem(defaultValue = "1000")
            int maxEntries = 1000;

            /**
             * Defines the limit of entries that should be kept in the cache
             */
            @ConfigItem(defaultValue = "30000")
            long lifespan = 30000;
        }

        @ConfigGroup
        public static class ClaimInformationPointConfig {

            /**
             *
             */
            @ConfigItem(name = ConfigItem.PARENT)
            Map<String, Map<String, Map<String, String>>> complexConfig;

            /**
             *
             */
            @ConfigItem(name = ConfigItem.PARENT)
            Map<String, Map<String, String>> simpleConfig;
        }
    }
}
