package io.quarkus.cache.runtime.noop;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.CacheManagerImpl;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NoOpCacheBuildRecorder {

    public void buildCaches(BeanContainer beanContainer, Set<String> cacheNames) {
        // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
        Map<String, Cache> caches = new HashMap<>(cacheNames.size() + 1, 1.0F);

        NoOpCache cache = new NoOpCache();
        for (String cacheName : cacheNames) {
            caches.put(cacheName, cache);
        }

        beanContainer.instance(CacheManagerImpl.class).setCaches(caches);
    }
}
