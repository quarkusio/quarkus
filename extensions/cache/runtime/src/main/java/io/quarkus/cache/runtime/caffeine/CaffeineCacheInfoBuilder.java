package io.quarkus.cache.runtime.caffeine;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.cache.runtime.CacheConfig;
import io.quarkus.cache.runtime.CacheConfig.CaffeineConfig.CaffeineNamespaceConfig;

public class CaffeineCacheInfoBuilder {

    public static Set<CaffeineCacheInfo> build(Set<String> cacheNames, CacheConfig cacheConfig) {
        if (cacheNames.isEmpty()) {
            return Collections.emptySet();
        } else {
            return cacheNames.stream().map(cacheName -> {
                CaffeineCacheInfo cacheInfo = new CaffeineCacheInfo();
                cacheInfo.name = cacheName;
                CaffeineNamespaceConfig namespaceConfig = cacheConfig.caffeine.namespace.get(cacheInfo.name);
                if (namespaceConfig != null) {
                    namespaceConfig.initialCapacity.ifPresent(capacity -> cacheInfo.initialCapacity = capacity);
                    namespaceConfig.maximumSize.ifPresent(size -> cacheInfo.maximumSize = size);
                    namespaceConfig.expireAfterWrite.ifPresent(delay -> cacheInfo.expireAfterWrite = delay);
                    namespaceConfig.expireAfterAccess.ifPresent(delay -> cacheInfo.expireAfterAccess = delay);
                    cacheInfo.metricsEnabled = namespaceConfig.metricsEnabled;
                }
                return cacheInfo;
            }).collect(Collectors.toSet());
        }
    }
}
