package io.quarkus.cache.runtime.augmented;

import static javax.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@AugmentedCacheResult
@Interceptor
@Priority(PLATFORM_BEFORE)
public class AugmentedCacheResultInterceptor extends AugmentedCacheAnnotationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(AugmentedCacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        AugmentedCacheResult annotation = getCacheAnnotation(context, AugmentedCacheResult.class);
        CaffeineCache cache = cacheRepository.getCache(annotation.cacheName());
        Object key = getCacheKey(context, cache.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, cache.getName());
        }
        return cache.get(key, () -> context.proceed(), annotation.lockTimeout());
    }
}
