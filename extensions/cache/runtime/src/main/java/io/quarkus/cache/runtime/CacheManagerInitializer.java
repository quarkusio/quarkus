package io.quarkus.cache.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

import io.quarkus.cache.CacheManager;

/**
 * This class is used to eagerly create the {@link CacheManager} bean instance at STATIC_INIT execution time.
 */
public class CacheManagerInitializer {

    private static void onStaticInit(@Observes @Initialized(ApplicationScoped.class) Object event, CacheManager cacheManager) {
        cacheManager.getCacheNames();
    }
}
