package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@CacheInvalidateAllInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY)
public class CacheInvalidateAllInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateAllInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        for (CacheInvalidateAllInterceptorBinding binding : getInterceptorBindings(context,
                CacheInvalidateAllInterceptorBinding.class)) {
            CaffeineCache cache = cacheRepository.getCache(binding.cacheName());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Invalidating all entries from cache [%s]", cache.getName());
            }
            cache.invalidateAll();
        }
        return context.proceed();
    }
}
