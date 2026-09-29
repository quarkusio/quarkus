package io.quarkus.cache.runtime;

import io.quarkus.cache.CacheResultPredicate;

/**
 * Default value of {@link io.quarkus.cache.CacheResult#unless()}: every result is cached.
 */
public class UndefinedCacheResultPredicate implements CacheResultPredicate {

    @Override
    public boolean test(Object result) {
        throw new UnsupportedOperationException("This cache result predicate should never be invoked");
    }
}
