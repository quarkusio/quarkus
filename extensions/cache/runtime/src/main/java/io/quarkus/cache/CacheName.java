package io.quarkus.cache;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 * <p>
 * Use this annotation on a field, a constructor parameter or a method parameter to inject a {@link Cache} and interact with it
 * programmatically e.g. store, retrieve or delete cache values.
 * </p>
 * <p>
 * Field injection example:
 *
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 *
 *     {@literal @}CacheName("my-cache")
 *     Cache cache;
 *
 *     String getExpensiveValue(Object key) {
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 *
 * Constructor parameter injection example:
 *
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 *
 *     private Cache cache;
 *
 *     public CachedService(@CacheName("my-cache") Cache cache) {
 *         this.cache = cache;
 *     }
 *
 *     String getExpensiveValue(Object key) {
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 *
 * Method parameter injection example:
 *
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 *
 *     private Cache cache;
 *
 *     {@literal @}Inject
 *     public void setCache(@CacheName("my-cache") Cache cache) {
 *         this.cache = cache;
 *     }
 *
 *     String getExpensiveValue(Object key) {
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see CacheManager
 */
@Qualifier
@Target({ FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
public @interface CacheName {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String value();
}
