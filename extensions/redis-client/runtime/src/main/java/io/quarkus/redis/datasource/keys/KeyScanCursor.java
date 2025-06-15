package io.quarkus.redis.datasource.keys;

import java.util.Set;

import io.quarkus.redis.datasource.Cursor;

public interface KeyScanCursor<K> extends Cursor<Set<K>> {
    /**
     * Returns an {@code Iterable} providing the keys individually. Unlike {@link #next()} which provides the keys by
     * batch, this method returns them one by one.
     *
     * @return the iterable
     */
    Iterable<K> toIterable();
}
