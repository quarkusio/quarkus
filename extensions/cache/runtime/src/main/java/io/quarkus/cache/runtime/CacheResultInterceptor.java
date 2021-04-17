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

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

@CacheResult(cacheName = "") // The `cacheName` attribute is @Nonbinding.
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);
    private static final String INTERCEPTOR_BINDING_ERROR_MSG = "The Quarkus cache extension is not working properly (CacheResult interceptor binding retrieval failed), please create a GitHub issue in the Quarkus repository to help the maintainers fix this bug";

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Throwable {
        CacheInterceptionContext<CacheResult> interceptionContext = getInterceptionContext(invocationContext,
                CacheResult.class, true);

        if (interceptionContext.getInterceptorBindings().isEmpty()) {
            // This should never happen.
            LOGGER.warn(INTERCEPTOR_BINDING_ERROR_MSG);
            return invocationContext.proceed();
        }

        CacheResult binding = interceptionContext.getInterceptorBindings().get(0);
        AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
        Object key = getCacheKey(cache, interceptionContext.getCacheKeyParameterPositions(), invocationContext.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());
        }

        try {

            CompletableFuture<Object> cacheValue = cache.get(key, new Function<Object, Object>() {
                @Override
                public Object apply(Object k) {
                    try {
                        if (Uni.class.isAssignableFrom(invocationContext.getMethod().getReturnType())) {
                            LOGGER.debugf("Adding %s entry with key [%s] into cache [%s]",
                                    UnresolvedUniValue.class.getSimpleName(), key, cache.getName());
                            return UnresolvedUniValue.INSTANCE;
                        } else {
                            return invocationContext.proceed();
                        }
                    } catch (Exception e) {
                        throw new CacheException(e);
                    }
                }
            });

            Object value;
            if (binding.lockTimeout() <= 0) {
                value = cacheValue.get();
            } else {
                try {
                    /*
                     * If the current thread started the cache value computation, then the computation is already finished since
                     * it was done synchronously and the following call will never time out.
                     */
                    value = cacheValue.get(binding.lockTimeout(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // TODO: Add statistics here to monitor the timeout.
                    return invocationContext.proceed();
                }
            }

            if (Uni.class.isAssignableFrom(invocationContext.getMethod().getReturnType())) {
                return resolveUni(invocationContext, cache, key, value);
            } else {
                return value;
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

    private Object resolveUni(InvocationContext invocationContext, AbstractCache cache, Object key, Object value)
            throws Exception {
        if (value == UnresolvedUniValue.INSTANCE) {
            return ((Uni<Object>) invocationContext.proceed())
                    .onItem().call(emittedValue -> cache.replaceUniValue(key, emittedValue));
        } else {
            return Uni.createFrom().item(value);
        }
    }
}
