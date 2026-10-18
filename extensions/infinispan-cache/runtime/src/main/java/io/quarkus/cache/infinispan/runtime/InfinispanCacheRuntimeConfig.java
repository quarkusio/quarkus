package io.quarkus.cache.infinispan.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface InfinispanCacheRuntimeConfig {
    /**
     * The default lifespan of the item stored in the cache
     */
    Optional<Duration> lifespan();

    /**
     * The default max-idle of the item stored in the cache
     */
    Optional<Duration> maxIdle();

}
