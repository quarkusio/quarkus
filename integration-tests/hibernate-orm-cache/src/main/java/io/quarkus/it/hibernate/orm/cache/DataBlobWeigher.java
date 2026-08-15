package io.quarkus.it.hibernate.orm.cache;

import com.github.benmanes.caffeine.cache.Weigher;

/**
 * Weigher that assigns weight based on estimated memory footprint.
 * Hibernate wraps entities in internal cache entry objects, so
 * the value is not the entity directly.
 */
public class DataBlobWeigher implements Weigher<Object, Object> {

    @Override
    public int weigh(Object key, Object value) {
        // Default weight for cache entries
        return 100;
    }
}
