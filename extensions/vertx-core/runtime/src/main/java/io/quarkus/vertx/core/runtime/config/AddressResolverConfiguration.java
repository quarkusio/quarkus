package io.quarkus.vertx.core.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AddressResolverConfiguration {

    /**
     * The maximum amount of time in seconds that a successfully resolved address will be cached.
     * <p>
     * If not set explicitly, resolved addresses may be cached forever.
     */
    @ConfigItem(defaultValue = "2147483647")
    public int cacheMaxTimeToLive;

    /**
     * The minimum amount of time in seconds that a successfully resolved address will be cached.
     */
    @ConfigItem(defaultValue = "0")
    public int cacheMinTimeToLive;

    /**
     * The amount of time in seconds that an unsuccessful attempt to resolve an address will be cached.
     */
    @ConfigItem(defaultValue = "0")
    public int cacheNegativeTimeToLive;

}
