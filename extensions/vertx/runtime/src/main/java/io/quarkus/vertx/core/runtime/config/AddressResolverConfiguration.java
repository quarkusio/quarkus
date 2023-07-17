package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;

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

    /**
     * The maximum number of queries to be sent during a resolution.
     */
    @ConfigItem(defaultValue = "4")
    public int maxQueries;

    /**
     * The duration after which a DNS query is considered to be failed.
     */
    @ConfigItem(defaultValue = "5S")
    public Duration queryTimeout;
}
