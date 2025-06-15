package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.Cursor;

public interface HashScanCursor<K, V> extends Cursor<Map<K, V>> {
    /**
     * Returns an {@code Iterable} providing each entry from the hash individually. Unlike {@link #next()} which
     * provides the entries by batch, this method returns them one by one.
     *
     * @return the iterable
     */
    Iterable<Map.Entry<K, V>> toIterable();
}
