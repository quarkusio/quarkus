package io.quarkus.cache.redis.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RedisCacheBuildTimeConfig {

    /**
     * The default type of the value stored in the cache.
     */
    @ConfigItem
    public Optional<String> valueType;

    /**
     * The key type, {@code String} by default.
     */
    @ConfigItem
    public Optional<String> keyType;
}
