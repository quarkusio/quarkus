package io.quarkus.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Use this interface to retrieve all existing {@link Cache} names and interact with any cache programmatically e.g.
 * store, retrieve or delete cache values. It shares the same caches collection the Quarkus caching annotations use. The
 * {@code @io.quarkus.cache.CacheName} annotation can also be used to inject and access a specific cache from its name.
 * </p>
 * <p>
 * Code example:
 *
 * <pre>
 * {@literal @}Singleton
 * public class CachedService {
 *
 *     private final CacheManager cacheManager;
 *
 *     public CachedService(CacheManager cacheManager) {
 *         this.cacheManager = cacheManager;
 *     }

 *     String getExpensiveValue(Object key) {
 *         Cache cache = cacheManager.getCache("my-cache");
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 * </p>
 */
public interface CacheManager {

    /**
     * Gets a collection of all cache names.
     *
     * @return names of all caches
     */
    Collection<String> getCacheNames();

    /**
     * Gets the cache identified by the given name.
     *
     * @param name
     *        cache name
     *
     * @return an {@link Optional} containing the identified cache if it exists, or an empty {@link Optional} otherwise
     */
    Optional<Cache> getCache(String name);
}
