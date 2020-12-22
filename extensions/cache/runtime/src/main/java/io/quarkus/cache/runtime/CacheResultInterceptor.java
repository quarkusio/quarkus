package io.quarkus.cache.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Throwable {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);

        AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
        short[] cacheKeyParameterPositions = getCacheKeyParameterPositions(context);
        Object key = getCacheKey(cache, cacheKeyParameterPositions, context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());
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

        } catch (ExecutionException e) {
            /*
             * Any exception raised during a cache computation will be encapsulated into an ExecutionException because it is
             * thrown during a CompletionStage execution.
             */
            if (e.getCause() instanceof CacheException) {
                /*
                 * The ExecutionException was caused by a CacheException (most likely case).
                 * Let's throw the CacheException cause if possible or the CacheException itself otherwise.
                 */
                if (e.getCause().getCause() != null) {
                    throw e.getCause().getCause();
                } else {
                    throw e.getCause();
                }
            } else if (e.getCause() != null) {
                // The ExecutionException was caused by another type of Throwable (unlikely case).
                throw e.getCause();
            } else {
                // The ExecutionException does not have a cause (very unlikely case).
                throw e;
            }
        }
    }
}
