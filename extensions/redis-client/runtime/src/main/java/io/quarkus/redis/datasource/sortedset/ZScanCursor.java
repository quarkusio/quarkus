package io.quarkus.redis.datasource.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.Cursor;

public interface ZScanCursor<V> extends Cursor<List<ScoredValue<V>>> {

    /**
     * Returns an {@code Iterable} providing each member of the sorted set individually. Unlike {@link #next()} which
     * provides the members by batch, this method returns them one by one.
     *
     * @return the iterable
     */
    Iterable<ScoredValue<V>> toIterable();
}
