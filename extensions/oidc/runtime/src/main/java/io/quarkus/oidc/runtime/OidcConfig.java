package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.oidc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OidcConfig {

    /**
     * The default tenant.
     */
    @WithParentName
    OidcTenantConfig defaultTenant();

    /**
     * Additional named tenants.
     */
    @ConfigDocSection
    @ConfigDocMapKey("tenant")
    @WithParentName
    Map<String, OidcTenantConfig> namedTenants();

    /**
     * Default TokenIntrospection and UserInfo Cache configuration which is used for all the tenants if it is enabled
     * with the build-time 'quarkus.oidc.default-token-cache-enabled' property ('true' by default) and also activated,
     * see its `max-size` property.
     */
    TokenCache tokenCache();

    /**
     * If OIDC tenants should be resolved using the bearer access token's issuer (`iss`) claim value.
     */
    @WithDefault("false")
    boolean resolveTenantsWithIssuer();

    /**
     * Default TokenIntrospection and UserInfo cache configuration.
     */
    interface TokenCache {
        /**
         * Maximum number of cache entries.
         * Set it to a positive value if the cache has to be enabled.
         */
        @WithDefault("0")
        int maxSize();

        /**
         * Maximum amount of time a given cache entry is valid for.
         */
        @WithDefault("3M")
        Duration timeToLive();

        /**
         * Clean up timer interval.
         * If this property is set then a timer will check and remove the stale entries periodically.
         */
        Optional<Duration> cleanUpTimerInterval();
    }
}
