package io.quarkus.cache.redis.runtime;

import java.time.Duration;
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


                if (namedRuntimeConfig != null && namedRuntimeConfig.computeTimeout.isPresent()) {
                    cacheInfo.computeTimeout = namedRuntimeConfig.computeTimeout.get();
                } else if (defaultRuntimeConfig.computeTimeout.isPresent()) {
                    cacheInfo.computeTimeout = defaultRuntimeConfig.computeTimeout.get();
                } else {
                    cacheInfo.computeTimeout = Duration.ofSeconds(10);
                }

                if (namedRuntimeConfig != null && namedRuntimeConfig.ttl.isPresent()) {
                    cacheInfo.ttl = namedRuntimeConfig.ttl;
                } else if (defaultRuntimeConfig.ttl.isPresent()) {
                    cacheInfo.ttl = defaultRuntimeConfig.ttl;
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
