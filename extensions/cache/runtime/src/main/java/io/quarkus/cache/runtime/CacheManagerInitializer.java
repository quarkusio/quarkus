package io.quarkus.cache.runtime;

import javax.enterprise.event.Observes;

import io.quarkus.cache.CacheManager;
import io.quarkus.runtime.StartupEvent;

/**
 * This class is used to eagerly create the {@link CacheManager} bean instance at RUNTIME_INIT execution time.
 */
public class CacheManagerInitializer {

    private static void onStartup(@Observes StartupEvent event, CacheManager cacheManager) {
        cacheManager.getCacheNames();
    }
}
