package io.quarkus.cache;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * <p>
 * Use this annotation on a field, a constructor parameter or a method parameter to inject a {@link Cache} and interact with it
 * programmatically.
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
 *     // Interact with the cache.
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
 *     // Interact with the cache.
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
 *     // Interact with the cache.
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
