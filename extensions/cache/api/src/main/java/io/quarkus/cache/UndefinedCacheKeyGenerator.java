package io.quarkus.cache;

import java.lang.reflect.Method;

/**
 * This {@link CacheKeyGenerator} implementation is ignored when a cache key is computed.
 */
public class UndefinedCacheKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(Method method, Object... methodParams) {
        throw new UnsupportedOperationException("This cache key generator should never be invoked");
    }
}
