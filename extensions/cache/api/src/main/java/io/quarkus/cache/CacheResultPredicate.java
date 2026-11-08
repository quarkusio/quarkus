package io.quarkus.cache;

/**
 * Decides, after a method annotated with {@link CacheResult} has been invoked, whether its result must be kept out of
 * the cache. Referenced from {@link CacheResult#unless()}.
 * <p>
 * An implementation can be a CDI bean, in which case it is resolved from the container, or a plain class with a
 * public no-arg constructor.
 */
@FunctionalInterface
public interface CacheResultPredicate {

    /**
     * @param result the value returned by the cached method, which may be {@code null}
     * @return {@code true} to remove the value from the cache so that the next invocation computes it again
     */
    boolean test(Object result);
}
