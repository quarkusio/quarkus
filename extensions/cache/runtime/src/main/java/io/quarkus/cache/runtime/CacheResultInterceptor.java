package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);
        Object key = buildCacheKey(binding.cacheName(), binding.cacheKeyParameterPositions(), context.getParameters());
        CaffeineCache cache = cacheRepository.getCache(binding.cacheName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, cache.getName());
        }
        return cache.get(key, () -> context.proceed(), binding.lockTimeout());
    }
}
