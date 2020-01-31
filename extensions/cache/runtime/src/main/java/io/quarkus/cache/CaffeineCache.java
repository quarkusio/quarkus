package io.quarkus.cache;

import java.util.Set;

public interface CaffeineCache extends Cache {

    /**
     * Returns an unmodifiable {@link Set} view of the keys contained in this cache. If the cache entries are modified while an
     * iteration over the set is in progress, the set will remain unchanged.
     *
     * @return a set view of the keys contained in this cache
     */
    Set<Object> keySet();
}
