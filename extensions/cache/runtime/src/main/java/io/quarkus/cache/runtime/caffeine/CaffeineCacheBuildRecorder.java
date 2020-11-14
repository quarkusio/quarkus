package io.quarkus.cache.runtime.caffeine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.cache.runtime.CacheRepository;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CaffeineCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheBuildRecorder.class);

    public void buildCaches(BeanContainer beanContainer, Set<CaffeineCacheInfo> cacheInfos) {
        // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
        Map<String, CaffeineCache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);

        for (CaffeineCacheInfo cacheInfo : cacheInfos) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(
                        "Building Caffeine cache [%s] with [initialCapacity=%s], [maximumSize=%s], [expireAfterWrite=%s] and [expireAfterAccess=%s]",
                        cacheInfo.name, cacheInfo.initialCapacity, cacheInfo.maximumSize, cacheInfo.expireAfterWrite,
                        cacheInfo.expireAfterAccess);
            }
            CaffeineCache cache = new CaffeineCache(cacheInfo);
            caches.put(cacheInfo.name, cache);
        }

        beanContainer.instance(CacheRepository.class).setCaches(caches);
    }
}
