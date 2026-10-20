package io.quarkus.cache.infinispan.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheManagerInfo;
import io.quarkus.cache.runtime.CacheManagerImpl;

public class InfinispanCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(InfinispanCacheBuildRecorder.class);

    public static CacheManagerInfo getCacheManagerSupplier(InfinispanCachesBuildTimeConfig buildConfig,
            InfinispanCachesConfig infinispanCacheConfig) {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && "infinispan".equals(context.cacheType());
            }

            @Override
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Supplier<CacheManager> get(Context context) {
                return new Supplier<CacheManager>() {
                    @Override
                    public CacheManager get() {
                        Set<InfinispanCacheInfo> cacheInfos = InfinispanCacheInfoBuilder.build(context.cacheNames(),
                                buildConfig,
                                infinispanCacheConfig);
                        if (cacheInfos.isEmpty()) {
                            return new CacheManagerImpl(Collections.emptyMap());
                        } else {
                            // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
                            Map<String, Cache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);
                            for (InfinispanCacheInfo cacheInfo : cacheInfos) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debugf(
                                            "Building Infinispan cache [%s] with [lifespan=%s], [maxIdle=%s]",
                                            cacheInfo.name, cacheInfo.lifespan, cacheInfo.maxIdle);
                                }

                                InfinispanCacheImpl cache = new InfinispanCacheImpl(cacheInfo, buildConfig.clientName());
                                caches.put(cacheInfo.name, cache);
                            }
                            return new CacheManagerImpl(caches);
                        }
                    }
                };
            }
        };
    }
}
