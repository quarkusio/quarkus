package io.quarkus.cache.runtime.caffeine;

import static io.quarkus.cache.runtime.CacheConfig.CaffeineConfig.CaffeineCacheConfig;

import java.util.Collections;
import java.util.Set;

import io.quarkus.cache.runtime.CacheConfig;
import io.quarkus.runtime.configuration.HashSetFactory;

public class CaffeineCacheInfoBuilder {

    public static Set<CaffeineCacheInfo> build(Set<String> cacheNames, CacheConfig cacheConfig) {
        if (cacheNames.isEmpty()) {
            return Collections.emptySet();
        } else {
            CaffeineCacheConfig defaultConfig = cacheConfig.caffeine().defaultConfig();

            Set<CaffeineCacheInfo> cacheInfos = HashSetFactory.<CaffeineCacheInfo> getInstance()
                    .apply(cacheNames.size());
            for (String cacheName : cacheNames) {

                CaffeineCacheInfo cacheInfo = new CaffeineCacheInfo();
                cacheInfo.name = cacheName;

                CaffeineCacheConfig namedCacheConfig = cacheConfig.caffeine().cachesConfig().get(cacheInfo.name);

                if (namedCacheConfig != null && namedCacheConfig.initialCapacity().isPresent()) {
                    cacheInfo.initialCapacity = namedCacheConfig.initialCapacity().getAsInt();
                } else if (defaultConfig.initialCapacity().isPresent()) {
                    cacheInfo.initialCapacity = defaultConfig.initialCapacity().getAsInt();
                }

                if (namedCacheConfig != null && namedCacheConfig.maximumSize().isPresent()) {
                    cacheInfo.maximumSize = namedCacheConfig.maximumSize().getAsLong();
                } else if (defaultConfig.maximumSize().isPresent()) {
                    cacheInfo.maximumSize = defaultConfig.maximumSize().getAsLong();
                }

                if (namedCacheConfig != null && namedCacheConfig.expireAfterWrite().isPresent()) {
                    cacheInfo.expireAfterWrite = namedCacheConfig.expireAfterWrite().get();
                } else if (defaultConfig.expireAfterWrite().isPresent()) {
                    cacheInfo.expireAfterWrite = defaultConfig.expireAfterWrite().get();
                }

                if (namedCacheConfig != null && namedCacheConfig.expireAfterAccess().isPresent()) {
                    cacheInfo.expireAfterAccess = namedCacheConfig.expireAfterAccess().get();
                } else if (defaultConfig.expireAfterAccess().isPresent()) {
                    cacheInfo.expireAfterAccess = defaultConfig.expireAfterAccess().get();
                }

                if (namedCacheConfig != null && namedCacheConfig.metricsEnabled().isPresent()) {
                    cacheInfo.metricsEnabled = namedCacheConfig.metricsEnabled().get();
                } else if (defaultConfig.metricsEnabled().isPresent()) {
                    cacheInfo.metricsEnabled = defaultConfig.metricsEnabled().get();
                }

                cacheInfos.add(cacheInfo);
            }
            return cacheInfos;
        }
    }
}
