package io.quarkus.cache.redis.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED, name = "cache.redis")
public class RedisCachesBuildTimeConfig {

    /**
     * The name of the named Redis client to be used for communicating with Redis.
     * If not set, use the default Redis client.
     */
    @ConfigItem
    public Optional<String> clientName;

    /**
     * Default configuration applied to all Redis caches (lowest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public RedisCacheBuildTimeConfig defaultConfig;

    /**
     * Additional configuration applied to a specific Redis cache (highest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    public Map<String, RedisCacheBuildTimeConfig> cachesConfig;

}
