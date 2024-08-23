package io.quarkus.cache.redis.runtime;

import java.util.*;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheManagerInfo;
import io.quarkus.cache.runtime.CacheManagerImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RedisCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(RedisCacheBuildRecorder.class);

    private final RedisCachesBuildTimeConfig buildConfig;
    private final RuntimeValue<RedisCachesConfig> redisCacheConfigRV;

    private static Map<String, String> valueTypes;

    public RedisCacheBuildRecorder(RedisCachesBuildTimeConfig buildConfig, RuntimeValue<RedisCachesConfig> redisCacheConfigRV) {
        this.buildConfig = buildConfig;
        this.redisCacheConfigRV = redisCacheConfigRV;
    }

    public CacheManagerInfo getCacheManagerSupplier() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && "redis".equals(context.cacheType()); // TODO: fix constant
            }

            @Override
            @SuppressWarnings({ "rawtypes", "unchecked" })
            public Supplier<CacheManager> get(Context context) {
                return new Supplier<CacheManager>() {
                    @Override
                    public CacheManager get() {
                        Set<RedisCacheInfo> cacheInfos = RedisCacheInfoBuilder.build(context.cacheNames(), buildConfig,
                                redisCacheConfigRV.getValue(), valueTypes);
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

                                RedisCacheImpl cache = new RedisCacheImpl(cacheInfo, buildConfig.clientName);
                                caches.put(cacheInfo.name, cache);
                            }
                            return new CacheManagerImpl(caches);
                        }
                    }
                };
            }
        };
    }

    public void setCacheValueTypes(Map<String, String> valueTypes) {
        RedisCacheBuildRecorder.valueTypes = valueTypes;
    }
}
