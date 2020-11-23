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

        } catch (ExecutionException e) {
            /*
             * Any exception raised during a cache computation will be encapsulated into an ExecutionException because it is
             * thrown during a CompletionStage execution.
             */
            if (e.getCause() instanceof CacheException) {
                // The ExecutionException was caused by a CacheException (most likely case).
                CacheException cacheException = (CacheException) e.getCause();
                // Let's see if we can throw the root cause of the exceptions chain.
                if (cacheException.getCause() instanceof Exception) {
                    // If it is an Exception, the root cause is thrown.
                    throw (Exception) cacheException.getCause();
                } else {
                    // If it is an Error, the CacheException itself is thrown because interceptors have to throw exceptions.
                    throw cacheException;
                }
            } else if (e.getCause() instanceof Exception) {
                // The ExecutionException was caused by another type of Exception (very unlikely case). Let's throw that cause!
                throw (Exception) e.getCause();
            } else {
                /*
                 * The ExecutionException was caused by an Error which can't be thrown because interceptors have to throw
                 * exceptions. The ExecutionException itself is thrown instead.
                 */
                throw e;
            }
        }
    }
}
