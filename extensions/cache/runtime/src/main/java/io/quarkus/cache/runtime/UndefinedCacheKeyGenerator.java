package io.quarkus.cache.runtime;

import java.lang.reflect.Method;

import io.quarkus.cache.CacheKeyGenerator;

/**
 * This {@link CacheKeyGenerator} implementation is ignored by {@link CacheInterceptor} when a cache key is computed.
 */
public class UndefinedCacheKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(Method method, Object... methodParams) {
        throw new UnsupportedOperationException("This cache key generator should never be invoked");
    }
}
