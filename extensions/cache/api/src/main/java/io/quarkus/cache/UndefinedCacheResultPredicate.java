package io.quarkus.cache;

/**
 * Default value of {@link CacheResult#unless()}: every result is cached.
 */
public class UndefinedCacheResultPredicate implements CacheResultPredicate {

    @Override
    public boolean test(Object result) {
        throw new UnsupportedOperationException("This cache result predicate should never be invoked");
    }
}
