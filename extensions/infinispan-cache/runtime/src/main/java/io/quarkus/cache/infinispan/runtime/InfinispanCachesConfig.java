package io.quarkus.cache.infinispan.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = RUN_TIME)
@ConfigMapping(prefix = "quarkus.cache.infinispan")
public interface InfinispanCachesConfig {

    /**
     * Default configuration applied to all Infinispan caches (lowest precedence)
     */
    @WithParentName
    InfinispanCacheRuntimeConfig defaultConfig();

    /**
     * Additional configuration applied to a specific Infinispan cache (highest precedence)
     */
    @WithParentName
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    Map<String, InfinispanCacheRuntimeConfig> cachesConfig();

}
