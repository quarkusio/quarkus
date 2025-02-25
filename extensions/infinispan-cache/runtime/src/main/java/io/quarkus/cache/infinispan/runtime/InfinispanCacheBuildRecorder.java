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
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class InfinispanCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(InfinispanCacheBuildRecorder.class);

    private final InfinispanCachesBuildTimeConfig buildConfig;
    private final RuntimeValue<InfinispanCachesConfig> infinispanCacheConfigRV;

    public InfinispanCacheBuildRecorder(InfinispanCachesBuildTimeConfig buildConfig,
            RuntimeValue<InfinispanCachesConfig> infinispanCacheConfigRV) {
        this.buildConfig = buildConfig;
        this.infinispanCacheConfigRV = infinispanCacheConfigRV;
    }

    public CacheManagerInfo getCacheManagerSupplier() {
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
                                infinispanCacheConfigRV.getValue());
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
