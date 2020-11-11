package io.quarkus.cache.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

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

        CaffeineCache cache = cacheRepository.getCache(binding.cacheName());
        Object key = getCacheKey(cache, binding.cacheKeyParameterPositions(), context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, cache.getName());
        }

        try {

            CompletableFuture<Object> cacheValue = cache.get(key, new Function<Object, Object>() {
                @Override
                public Object apply(Object k) {
                    try {
                        return context.proceed();
                    } catch (Exception e) {
                        throw new CacheException(e);
                    }
                }
            });

            if (binding.lockTimeout() <= 0) {
                return cacheValue.get();
            } else {
                try {
                    /*
                     * If the current thread started the cache value computation, then the computation is already finished since
                     * it was done synchronously and the following call will never time out.
                     */
                    return cacheValue.get(binding.lockTimeout(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // TODO: Add statistics here to monitor the timeout.
                    return context.proceed();
                }
            }

        } catch (CacheException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw e;
            }
        }
    }
}
