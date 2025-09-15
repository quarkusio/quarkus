package io.quarkus.cache.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.cache")
public interface CacheBuildConfig {

    String CAFFEINE_CACHE_TYPE = "caffeine";

    /**
     * Default cache type (backend provider). If no explicit type is defined for a cache, this type will be used.
     */
    @WithDefault(CAFFEINE_CACHE_TYPE)
    String type();

    /**
     * Configuration that allows customizing cache names to use a different type.
     */
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    @WithParentName
    Map<String, CacheTypeBuildConfig> cacheTypeByName();

    /**
     * Configuration that allows customizing cache names to use a different type.
     */
    @ConfigGroup
    interface CacheTypeBuildConfig {

        /**
         * Cache type to be used for the cache name.
         */
        @WithDefault(CAFFEINE_CACHE_TYPE)
        String type();
    }
}
