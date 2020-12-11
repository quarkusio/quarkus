package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

@CacheInvalidateAllInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY)
public class CacheInvalidateAllInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateAllInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        for (CacheInvalidateAllInterceptorBinding binding : getInterceptorBindings(context,
                CacheInvalidateAllInterceptorBinding.class)) {
            AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Invalidating all entries from cache [%s]", binding.cacheName());
            }
            cache.invalidateAll();
        }
        return context.proceed();
    }
}
