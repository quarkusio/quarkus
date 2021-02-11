package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheInvalidate;

@CacheInvalidate(cacheName = "") // The `cacheName` attribute is @Nonbinding.
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 1)
public class CacheInvalidateInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateInterceptor.class);
    private static final String INTERCEPTOR_BINDINGS_ERROR_MSG = "The Quarkus cache extension is not working properly (CacheInvalidate interceptor bindings retrieval failed), please create a GitHub issue in the Quarkus repository to help the maintainers fix this bug";

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        CacheInterceptionContext<CacheInvalidate> interceptionContext = getInterceptionContext(invocationContext,
                CacheInvalidate.class, true);
        if (interceptionContext.getInterceptorBindings().isEmpty()) {
            // This should never happen.
            LOGGER.warn(INTERCEPTOR_BINDINGS_ERROR_MSG);
        } else {
            Object key = null;
            for (CacheInvalidate binding : interceptionContext.getInterceptorBindings()) {
                AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
                if (key == null) {
                    key = getCacheKey(cache, interceptionContext.getCacheKeyParameterPositions(),
                            invocationContext.getParameters());
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Invalidating entry with key [%s] from cache [%s]", key, binding.cacheName());
                }
                cache.invalidate(key);
            }
        }
        return invocationContext.proceed();
    }
}
