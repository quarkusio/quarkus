package io.quarkus.cache.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Configuration that allows customizing cache names to use a different type.
 */
@ConfigGroup
public interface CacheTypeBuildConfig {

    /**
     * Cache type to be used for the cache name.
     */
    @WithDefault("")
    String type();
}
