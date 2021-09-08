package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "oidc", phase = ConfigPhase.RUN_TIME)
public class OidcConfig {

    /**
     * The default tenant.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public OidcTenantConfig defaultTenant;

    /**
     * Additional named tenants.
     */
    @ConfigDocSection
    @ConfigDocMapKey("tenant")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, OidcTenantConfig> namedTenants;

    /**
     * Default TokenIntrospection and UserInfo Cache configuration which is used for all the tenants if it is enabled.
     */
    @ConfigItem
    public TokenCache tokenCache = new TokenCache();

    /**
     * Default TokenIntrospection and UserInfo cache configuration.
     */
    @ConfigGroup
    public static class TokenCache {
        /**
         * Maximum number of cache entries.
         * Set it to a positive value if the cache has to be enabled.
         */
        @ConfigItem(defaultValue = "0")
        public int maxSize = 0;

        /**
         * Maximum amount of time a given cache entry is valid for.
         */
        @ConfigItem(defaultValue = "3M")
        public Duration timeToLive = Duration.ofMinutes(3);

        /**
         * Clean up timer interval.
         * If this property is set then a timer will check and remove the stale entries periodically.
         */
        @ConfigItem
        public Optional<Duration> cleanUpTimerInterval = Optional.empty();
    }
}
