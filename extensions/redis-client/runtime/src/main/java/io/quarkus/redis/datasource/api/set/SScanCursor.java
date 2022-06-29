package io.quarkus.redis.datasource.api.set;

import java.util.List;

import io.quarkus.redis.datasource.api.Cursor;

public interface SScanCursor<V> extends Cursor<List<V>> {
}
