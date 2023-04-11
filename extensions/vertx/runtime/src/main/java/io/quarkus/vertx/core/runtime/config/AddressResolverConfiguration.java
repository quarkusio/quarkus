package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface AddressResolverConfiguration {

    /**
     * The maximum amount of time in seconds that a successfully resolved address will be cached.
     * <p>
     * If not set explicitly, resolved addresses may be cached forever.
     */
    @WithDefault("2147483647")
    int cacheMaxTimeToLive();

    /**
     * The minimum amount of time in seconds that a successfully resolved address will be cached.
     */
    @WithDefault("0")
    int cacheMinTimeToLive();

    /**
     * The amount of time in seconds that an unsuccessful attempt to resolve an address will be cached.
     */
    @WithDefault("0")
    int cacheNegativeTimeToLive();

    /**
     * The maximum number of queries to be sent during a resolution.
     */
    @WithDefault("4")
    int maxQueries();

    /**
     * The duration after which a DNS query is considered to be failed.
     */
    @WithDefault("5S")
    Duration queryTimeout();
}
