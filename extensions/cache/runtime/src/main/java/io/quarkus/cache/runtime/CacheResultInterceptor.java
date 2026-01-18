package io.quarkus.cache.runtime;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

@CacheResult(cacheName = "") // The `cacheName` attribute is @Nonbinding.
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);
    private static final String INTERCEPTOR_BINDING_ERROR_MSG = "The Quarkus cache extension is not working properly (CacheResult interceptor binding retrieval failed), please create a GitHub issue in the Quarkus repository to help the maintainers fix this bug";

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Throwable {
        /*
         * io.smallrye.mutiny.Multi values are never cached.
         * There's already a WARN log entry at build time so we don't need to log anything at run time.
         */
        if (Multi.class.isAssignableFrom(invocationContext.getMethod().getReturnType())) {
            return invocationContext.proceed();
        }

        CacheInterceptionContext<CacheResult> interceptionContext = getInterceptionContext(invocationContext,
                CacheResult.class, true);

        if (interceptionContext.getInterceptorBindings().isEmpty()) {
            // This should never happen.
            LOGGER.warn(INTERCEPTOR_BINDING_ERROR_MSG);
            return invocationContext.proceed();
        }

        CacheResult binding = interceptionContext.getInterceptorBindings().get(0);
        AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
        Object key = getCacheKey(cache, binding.keyGenerator(), interceptionContext.getCacheKeyParameterPositions(),
                invocationContext.getMethod(), invocationContext.getParameters());
        LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());

        try {
            ReturnType returnType = determineReturnType(invocationContext.getMethod().getReturnType());
            if (returnType != ReturnType.NonAsync) {
                return binding.skipGet()
                        ? computeAlwaysAsync(invocationContext, binding, returnType, cache, key)
                        : computeIfAbsentAsync(invocationContext, binding, returnType, cache, key);
            } else {
                return binding.skipGet()
                        ? computeAlways(invocationContext, binding, cache, key)
                        : computeIfAbsent(invocationContext, binding, cache, key);
            }

        } catch (CacheException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }

    private Object computeAlwaysAsync(final InvocationContext invocationContext,
            final CacheResult binding,
            final ReturnType returnType,
            final AbstractCache cache,
            final Object key) throws Exception {

        Uni<Object> cacheValue = asyncInvocationResultToUni(invocationContext.proceed(), returnType)
                .chain(freshValue -> {
                    if (freshValue != null) {
                        Uni<Object> cacheUni = cache.put(key, freshValue)
                                .onItemOrFailure()
                                .transformToUni((v, t) -> Uni.createFrom().item(freshValue));

                        if (binding.lockTimeout() <= 0) {
                            return cacheUni;
                        }

                        return cacheUni.ifNoItem().after(Duration.ofMillis(binding.lockTimeout()))
                                .recoverWithItem(() -> freshValue);
                    }

                    return Uni.createFrom().nullItem();
                });

        return createAsyncResult(cacheValue, returnType);
    }

    private Object computeAlways(final InvocationContext invocationContext,
            final CacheResult binding,
            final AbstractCache cache,
            final Object key) throws Exception {

        Object value = invocationContext.proceed();

        if (value != null) {
            try {
                Uni<Void> cacheSetUni = cache.put(key, value);

                if (binding.lockTimeout() <= 0) {
                    cacheSetUni.await().indefinitely();
                } else {
                    cacheSetUni.await().atMost(Duration.ofMillis(binding.lockTimeout()));
                }
            } catch (TimeoutException ignore) {
                // TODO: Add statistics here to monitor the timeout.
            }
        }

        return value;
    }

    private Object computeIfAbsentAsync(final InvocationContext invocationContext,
            final CacheResult binding,
            final ReturnType returnType,
            final AbstractCache cache,
            final Object key) {

        Uni<Object> cacheValue = cache.getAsync(key, new Function<Object, Uni<Object>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Uni<Object> apply(Object key) {
                try {
                    return (Uni<Object>) asyncInvocationResultToUni(invocationContext.proceed(), returnType);
                } catch (CacheException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            }
        }).onFailure().call(new Function<>() {
            @Override
            public Uni<?> apply(Throwable throwable) {
                return cache.invalidate(key).replaceWith(throwable);
            }
        });

        if (binding.lockTimeout() <= 0) {
            return createAsyncResult(cacheValue, returnType);
        }
        // IMPORTANT: The item/failure are emitted on the captured context.
        cacheValue = cacheValue.ifNoItem().after(Duration.ofMillis(binding.lockTimeout()))
                .recoverWithUni(new Supplier<Uni<?>>() {
                    @Override
                    public Uni<?> get() {
                        try {
                            return asyncInvocationResultToUni(invocationContext.proceed(), returnType);
                        } catch (CacheException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new CacheException(e);
                        }
                    }
                });
        return createAsyncResult(cacheValue, returnType);
    }

    private Object computeIfAbsent(final InvocationContext invocationContext,
            final CacheResult binding,
            final AbstractCache cache,
            final Object key) throws Exception {

        Uni<Object> cacheValue = cache.get(key, new Function<Object, Object>() {
            @Override
            public Object apply(Object k) {
                try {
                    LOGGER.debugf("Adding entry with key [%s] into cache [%s]",
                            key, binding.cacheName());
                    return invocationContext.proceed();
                } catch (CacheException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new CacheException(e);
                }
            }
        });
        Object value;
        if (binding.lockTimeout() <= 0) {
            value = cacheValue.await().indefinitely();
        } else {
            try {
                /*
                 * If the current thread started the cache value computation, then the computation is already finished
                 * since
                 * it was done synchronously and the following call will never time out.
                 */
                value = cacheValue.await().atMost(Duration.ofMillis(binding.lockTimeout()));
            } catch (TimeoutException e) {
                // TODO: Add statistics here to monitor the timeout.
                return invocationContext.proceed();
            }
        }
        return value;
    }
}
