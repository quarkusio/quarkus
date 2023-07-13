package io.quarkus.cache.redis.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.configuration.HashSetFactory;

public class RedisCacheInfoBuilder {

    public static Set<RedisCacheInfo> build(Set<String> cacheNames, RedisCachesBuildTimeConfig buildTimeConfig,
            RedisCachesConfig runtimeConfig, Map<String, String> valueTypes) {
        if (cacheNames.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<RedisCacheInfo> result = HashSetFactory.<RedisCacheInfo> getInstance().apply(cacheNames.size());
            ;
            for (String cacheName : cacheNames) {

                RedisCacheInfo cacheInfo = new RedisCacheInfo();
                cacheInfo.name = cacheName;

                RedisCacheRuntimeConfig defaultRuntimeConfig = runtimeConfig.defaultConfig;
                RedisCacheRuntimeConfig namedRuntimeConfig = runtimeConfig.cachesConfig.get(cacheInfo.name);

                if (namedRuntimeConfig != null && namedRuntimeConfig.expireAfterAccess.isPresent()) {
                    cacheInfo.expireAfterAccess = namedRuntimeConfig.expireAfterAccess;
                } else if (defaultRuntimeConfig.expireAfterAccess.isPresent()) {
                    cacheInfo.expireAfterAccess = defaultRuntimeConfig.expireAfterAccess;
                }

                if (namedRuntimeConfig != null && namedRuntimeConfig.expireAfterWrite.isPresent()) {
                    cacheInfo.expireAfterWrite = namedRuntimeConfig.expireAfterWrite;
                } else if (defaultRuntimeConfig.expireAfterAccess.isPresent()) {
                    cacheInfo.expireAfterWrite = defaultRuntimeConfig.expireAfterWrite;
                }

                // Handle the deprecated TTL
                if (namedRuntimeConfig != null && namedRuntimeConfig.ttl.isPresent()) {
                    cacheInfo.expireAfterWrite = namedRuntimeConfig.ttl;
                } else if (defaultRuntimeConfig.ttl.isPresent()) {
                    cacheInfo.expireAfterWrite = defaultRuntimeConfig.ttl;
                }

                if (namedRuntimeConfig != null && namedRuntimeConfig.prefix.isPresent()) {
                    cacheInfo.prefix = namedRuntimeConfig.prefix.get();
                } else if (defaultRuntimeConfig.prefix.isPresent()) {
                    cacheInfo.prefix = defaultRuntimeConfig.prefix.get();
                }

                cacheInfo.valueType = valueTypes.get(cacheName);

                RedisCacheBuildTimeConfig defaultBuildTimeConfig = buildTimeConfig.defaultConfig;
                RedisCacheBuildTimeConfig namedBuildTimeConfig = buildTimeConfig.cachesConfig
                        .get(cacheInfo.name);

                if (namedBuildTimeConfig != null && namedBuildTimeConfig.keyType.isPresent()) {
                    cacheInfo.keyType = namedBuildTimeConfig.keyType.get();
                } else if (defaultBuildTimeConfig.keyType.isPresent()) {
                    cacheInfo.keyType = defaultBuildTimeConfig.keyType.get();
                }

                if (namedRuntimeConfig != null && namedRuntimeConfig.useOptimisticLocking.isPresent()) {
                    cacheInfo.useOptimisticLocking = namedRuntimeConfig.useOptimisticLocking.get();
                } else if (defaultRuntimeConfig.useOptimisticLocking.isPresent()) {
                    cacheInfo.useOptimisticLocking = defaultRuntimeConfig.useOptimisticLocking.get();
                }

                result.add(cacheInfo);
            }
            return result;
        }
    }
}
