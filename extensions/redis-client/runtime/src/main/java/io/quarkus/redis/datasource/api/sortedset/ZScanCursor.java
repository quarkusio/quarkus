package io.quarkus.redis.datasource.api.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.api.Cursor;

public interface ZScanCursor<V> extends Cursor<List<ScoredValue<V>>> {
}
