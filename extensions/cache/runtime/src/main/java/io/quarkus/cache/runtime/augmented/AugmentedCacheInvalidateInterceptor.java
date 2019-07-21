package io.quarkus.cache.runtime.augmented;

import static javax.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@AugmentedCacheInvalidate
@Interceptor
@Priority(PLATFORM_BEFORE)
public class AugmentedCacheInvalidateInterceptor extends AugmentedCacheAnnotationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(AugmentedCacheInvalidateInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        AugmentedCacheInvalidate annotation = getCacheAnnotation(context, AugmentedCacheInvalidate.class);
        CaffeineCache cache = cacheRepository.getCache(annotation.cacheName());
        Object key = getCacheKey(context, cache.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Invalidating entry with key [%s] from cache [%s]", key, cache.getName());
        }
        cache.invalidate(key);
        return context.proceed();
    }
}
