package io.quarkus.cache.redis.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface RedisCacheBuildTimeConfig {

    /**
     * The default type of the value stored in the cache.
     */
    Optional<String> valueType();

    /**
     * The key type, {@code String} by default.
     */
    Optional<String> keyType();
}
