package io.quarkus.cache.infinispan.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class InfinispanCacheRuntimeConfig {
    /**
     * The default lifespan of the item stored in the cache
     */
    @ConfigItem
    public Optional<Duration> lifespan;

    /**
     * The default max-idle of the item stored in the cache
     */
    @ConfigItem
    public Optional<Duration> maxIdle;

}
