package io.quarkus.cache.runtime;

import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheInvalidate;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

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
            return invocationContext.proceed();
        }
        ReturnType returnType = determineReturnType(invocationContext.getMethod().getReturnType());
        if (returnType == ReturnType.NonAsync) {
            return invalidateBlocking(invocationContext, interceptionContext);
        } else {
            return invalidateNonBlocking(invocationContext, interceptionContext, returnType);
        }
    }

    private Object invalidateNonBlocking(InvocationContext invocationContext,
            CacheInterceptionContext<CacheInvalidate> interceptionContext,
            ReturnType returnType) {
        LOGGER.trace("Invalidating cache entries in a non-blocking way");
        var uni = Multi.createFrom().iterable(interceptionContext.getInterceptorBindings())
                .onItem().transformToUniAndMerge(new Function<CacheInvalidate, Uni<? extends Void>>() {
                    @Override
                    public Uni<Void> apply(CacheInvalidate binding) {
                        return invalidate(binding, interceptionContext.getCacheKeyParameterPositions(), invocationContext);
                    }
                })
                .onItem().ignoreAsUni()
                .onItem().transformToUni(new Function<Object, Uni<?>>() {
                    @Override
                    public Uni<?> apply(Object ignored) {
                        try {
                            return asyncInvocationResultToUni(invocationContext.proceed(), returnType);
                        } catch (Exception e) {
                            throw new CacheException(e);
                        }
                    }
                });
        return createAsyncResult(uni, returnType);
    }

    private Object invalidateBlocking(InvocationContext invocationContext,
            CacheInterceptionContext<CacheInvalidate> interceptionContext) throws Exception {
        LOGGER.trace("Invalidating cache entries in a blocking way");
        for (CacheInvalidate binding : interceptionContext.getInterceptorBindings()) {
            invalidate(binding, interceptionContext.getCacheKeyParameterPositions(), invocationContext).await().indefinitely();
        }
        return invocationContext.proceed();
    }

    private Uni<Void> invalidate(CacheInvalidate binding, List<Short> cacheKeyParameterPositions,
            InvocationContext invocationContext) {
        Cache cache = cacheManager.getCache(binding.cacheName()).get();
        Object key = getCacheKey(cache, binding.keyGenerator(), cacheKeyParameterPositions, invocationContext.getMethod(),
                invocationContext.getParameters());
        LOGGER.debugf("Invalidating entry with key [%s] from cache [%s]", key, binding.cacheName());
        return cache.invalidate(key);
    }
}
