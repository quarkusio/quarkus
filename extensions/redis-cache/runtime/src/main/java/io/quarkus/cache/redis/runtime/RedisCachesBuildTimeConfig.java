package io.quarkus.cache.redis.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.cache.redis")
public interface RedisCachesBuildTimeConfig {

    /**
     * The name of the named Redis client to be used for communicating with Redis.
     * If not set, use the default Redis client.
     */
    Optional<String> clientName();

    /**
     * Default configuration applied to all Redis caches (lowest precedence)
     */
    @WithParentName
    RedisCacheBuildTimeConfig defaultConfig();

    /**
     * Additional configuration applied to a specific Redis cache (highest precedence)
     */
    @WithParentName
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    Map<String, RedisCacheBuildTimeConfig> cachesConfig();

}
