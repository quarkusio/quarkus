package io.quarkus.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Use this interface to retrieve all existing {@link Cache} names and interact with any cache programmatically. It shares the
 * same cache collection the Quarkus caching annotations use. The {@link CacheName} annotation can also be used to inject and
 * access a specific cache from its name.
 * </p>
 * <p>
 * Code example:
 * 
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 * 
 *     {@literal @}Inject
 *     CacheManager cacheManager;
 *     
 *     void doSomething() {
 *         Cache cache = cacheManager.getCache("my-cache");
 *         // Interact with the cache.
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
     * @param name cache name
     * @return an {@link Optional} containing the identified cache if it exists, or an empty {@link Optional} otherwise
     */
    Optional<Cache> getCache(String name);
}
