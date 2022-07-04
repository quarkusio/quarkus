package io.quarkus.redis.datasource.set;

import java.util.List;

import io.quarkus.redis.datasource.Cursor;

public interface SScanCursor<V> extends Cursor<List<V>> {
}
