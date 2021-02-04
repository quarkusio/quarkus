package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheInvalidateAll;

@CacheInvalidateAll(cacheName = "") // The `cacheName` attribute is @Nonbinding.
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY)
public class CacheInvalidateAllInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateAllInterceptor.class);
    private static final String INTERCEPTOR_BINDINGS_ERROR_MSG = "The Quarkus cache extension is not working properly (CacheInvalidateAll interceptor bindings retrieval failed), please create a GitHub issue in the Quarkus repository to help the maintainers fix this bug";

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        CacheInterceptionContext<CacheInvalidateAll> interceptionContext = getInterceptionContext(invocationContext,
                CacheInvalidateAll.class, false);
        if (interceptionContext.getInterceptorBindings().isEmpty()) {
            // This should never happen.
            LOGGER.warn(INTERCEPTOR_BINDINGS_ERROR_MSG);
        } else {
            for (CacheInvalidateAll binding : interceptionContext.getInterceptorBindings()) {
                AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Invalidating all entries from cache [%s]", binding.cacheName());
                }
                cache.invalidateAll();
            }
        }
        return invocationContext.proceed();
    }
}
