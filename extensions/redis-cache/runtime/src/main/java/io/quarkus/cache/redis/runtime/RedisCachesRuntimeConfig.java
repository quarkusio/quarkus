package io.quarkus.cache.redis.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = RUN_TIME)
@ConfigMapping(prefix = "quarkus.cache.redis")
public interface RedisCachesRuntimeConfig {

    /**
     * Default configuration applied to all Redis caches (lowest precedence)
     */
    @WithParentName
    RedisCacheRuntimeConfig defaultConfig();

    /**
     * Additional configuration applied to a specific Redis cache (highest precedence)
     */
    @WithParentName
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    Map<String, RedisCacheRuntimeConfig> cachesConfig();

}
