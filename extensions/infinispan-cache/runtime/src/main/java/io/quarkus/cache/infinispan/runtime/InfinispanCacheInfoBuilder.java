package io.quarkus.cache.infinispan.runtime;

import java.util.Collections;
import java.util.Set;

import io.quarkus.runtime.configuration.HashSetFactory;

public class InfinispanCacheInfoBuilder {

    public static Set<InfinispanCacheInfo> build(Set<String> cacheNames, InfinispanCachesBuildTimeConfig buildTimeConfig,
            InfinispanCachesConfig runtimeConfig) {
        if (cacheNames.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<InfinispanCacheInfo> result = HashSetFactory.<InfinispanCacheInfo> getInstance().apply(cacheNames.size());

            for (String cacheName : cacheNames) {

                InfinispanCacheInfo cacheInfo = new InfinispanCacheInfo();
                cacheInfo.name = cacheName;

                InfinispanCacheRuntimeConfig defaultRuntimeConfig = runtimeConfig.defaultConfig();
                InfinispanCacheRuntimeConfig namedRuntimeConfig = runtimeConfig.cachesConfig().get(cacheInfo.name);

                if (namedRuntimeConfig != null && namedRuntimeConfig.lifespan().isPresent()) {
                    cacheInfo.lifespan = namedRuntimeConfig.lifespan();
                } else if (defaultRuntimeConfig.lifespan().isPresent()) {
                    cacheInfo.lifespan = defaultRuntimeConfig.lifespan();
                }

                if (namedRuntimeConfig != null && namedRuntimeConfig.maxIdle().isPresent()) {
                    cacheInfo.maxIdle = namedRuntimeConfig.maxIdle();
                } else if (defaultRuntimeConfig.maxIdle().isPresent()) {
                    cacheInfo.maxIdle = defaultRuntimeConfig.maxIdle();
                }

                result.add(cacheInfo);
            }
            return result;
        }
    }
}
