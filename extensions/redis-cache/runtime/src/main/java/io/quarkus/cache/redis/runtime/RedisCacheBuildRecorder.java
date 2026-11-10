package io.quarkus.cache.redis.runtime;

import java.lang.reflect.Type;
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

public class RedisCacheBuildRecorder {

    public static final String REDIS_CACHE_TYPE = "redis";

    private static final Logger LOGGER = Logger.getLogger(RedisCacheBuildRecorder.class);

    public static CacheManagerInfo getCacheManagerSupplier(RedisCachesBuildTimeConfig buildConfig,
            RedisCachesRuntimeConfig redisCacheConfig, Map<String, Type> keyTypes, Map<String, Type> valueTypes) {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && REDIS_CACHE_TYPE.equals(context.cacheType());
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return new Supplier<CacheManager>() {
                    @Override
                    public CacheManager get() {
                        Set<RedisCacheInfo> cacheInfos = RedisCacheInfoBuilder.build(context.cacheNames(),
                                redisCacheConfig, keyTypes, valueTypes);
                        if (cacheInfos.isEmpty()) {
                            return new CacheManagerImpl(Collections.emptyMap());
                        } else {
                            // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
                            Map<String, Cache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);
                            for (RedisCacheInfo cacheInfo : cacheInfos) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debugf(
                                            "Building Redis cache [%s] with [ttl=%s], [prefix=%s], [classOfItems=%s]",
                                            cacheInfo.name, cacheInfo.expireAfterAccess, cacheInfo.prefix,
                                            cacheInfo.valueType);
                                }

                                RedisCacheImpl cache = new RedisCacheImpl(cacheInfo, buildConfig.clientName());
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
