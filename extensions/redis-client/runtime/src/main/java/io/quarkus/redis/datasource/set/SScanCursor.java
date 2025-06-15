package io.quarkus.redis.datasource.set;

import java.util.List;

import io.quarkus.redis.datasource.Cursor;

public interface SScanCursor<V> extends Cursor<List<V>> {

    /**
     * Returns an {@code Iterable} providing each member of the set individually. Unlike {@link #next()} which provides
     * the members by batch, this method returns them one by one.
     *
     * @return the iterable
     */
    Iterable<V> toIterable();
}
