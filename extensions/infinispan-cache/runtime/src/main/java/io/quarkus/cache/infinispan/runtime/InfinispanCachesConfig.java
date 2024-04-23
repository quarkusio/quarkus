package io.quarkus.cache.infinispan.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = RUN_TIME, name = "cache.infinispan")
public class InfinispanCachesConfig {

    /**
     * Default configuration applied to all Infinispan caches (lowest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public InfinispanCacheRuntimeConfig defaultConfig;

    /**
     * Additional configuration applied to a specific Infinispan cache (highest precedence)
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocMapKey("cache-name")
    @ConfigDocSection
    Map<String, InfinispanCacheRuntimeConfig> cachesConfig;

}
