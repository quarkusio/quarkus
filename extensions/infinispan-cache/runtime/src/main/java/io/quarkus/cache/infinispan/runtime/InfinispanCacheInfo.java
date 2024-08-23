package io.quarkus.cache.infinispan.runtime;

import java.time.Duration;
import java.util.Optional;

public class InfinispanCacheInfo {

    /**
     * The cache name
     */
    public String name;

    /**
     * The default lifespan of the item stored in the cache
     */
    public Optional<Duration> lifespan = Optional.empty();

    /**
     * The default max-idle of the item stored in the cache
     */
    public Optional<Duration> maxIdle = Optional.empty();

}
