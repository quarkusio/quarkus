package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@CacheInvalidateInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 1)
public class CacheInvalidateInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object key = null;
        for (CacheInvalidateInterceptorBinding binding : getInterceptorBindings(context,
                CacheInvalidateInterceptorBinding.class)) {
            if (key == null) {
                key = buildCacheKey(binding.cacheName(), binding.cacheKeyParameterPositions(), context.getParameters());
            }
            CaffeineCache cache = cacheRepository.getCache(binding.cacheName());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Invalidating entry with key [%s] from cache [%s]", key, cache.getName());
            }
            cache.invalidate(key);
        }
        return context.proceed();
    }
}
