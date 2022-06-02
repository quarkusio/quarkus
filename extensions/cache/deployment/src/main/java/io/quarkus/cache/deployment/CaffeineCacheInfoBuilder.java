package io.quarkus.cache.deployment;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.cache.deployment.CacheConfig.CaffeineConfig.CaffeineNamespaceConfig;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;

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
                    namespaceConfig.refreshAfterWrite.ifPresent(delay -> cacheInfo.refreshAfterWrite = delay);
                    cacheInfo.metricsEnabled = namespaceConfig.metricsEnabled;
                }
                return cacheInfo;
            }).collect(Collectors.toSet());
        }
    }
}
