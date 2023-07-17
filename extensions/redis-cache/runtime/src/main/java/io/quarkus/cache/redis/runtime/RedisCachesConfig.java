package io.quarkus.cache.redis.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = RUN_TIME, name = "cache.redis")
public class RedisCachesConfig {

    /**
     * Default configuration applied to all Redis caches (lowest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public RedisCacheRuntimeConfig defaultConfig;

    /**
     * Additional configuration applied to a specific Redis cache (highest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    Map<String, RedisCacheRuntimeConfig> cachesConfig;

}
