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
                Uni<Object> cacheValue = cache.get(key, new Function<Object, Object>() {
                    @Override
                    public Object apply(Object k) {
                        LOGGER.debugf("Adding %s entry with key [%s] into cache [%s]",
                                UnresolvedUniValue.class.getSimpleName(), key, binding.cacheName());
                        return UnresolvedUniValue.INSTANCE;
                    }
                }).onItem().transformToUni(new Function<Object, Uni<?>>() {
                    @Override
                    public Uni<?> apply(Object value) {
                        if (value == UnresolvedUniValue.INSTANCE) {
                            try {
                                return asyncInvocationResultToUni(invocationContext.proceed(), returnType)
                                        .call(new Function<Object, Uni<?>>() {
                                            @Override
                                            public Uni<?> apply(Object emittedValue) {
                                                return cache.replaceUniValue(key, emittedValue);
                                            }
                                        });
                            } catch (CacheException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new CacheException(e);
                            }
                        } else {
                            return Uni.createFrom().item(value);
                        }
                    }
                });
                if (binding.lockTimeout() <= 0) {
                    return createAsyncResult(cacheValue, returnType);
                }
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
            } else {
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

        } catch (CacheException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }

}
