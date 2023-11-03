package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;

@ApplicationScoped
public class CacheProducer {

    @Inject
    CacheManager cacheManager;

    @Produces
    @CacheName("") // The `value` attribute is @Nonbinding.
    Cache produce(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier instanceof CacheName) {
                return cacheManager.getCache(((CacheName) qualifier).value()).get();
            }
        }
        // This will never be returned.
        return null;
    }
}
