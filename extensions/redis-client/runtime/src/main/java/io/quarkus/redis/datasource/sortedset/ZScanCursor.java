package io.quarkus.redis.datasource.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.Cursor;

public interface ZScanCursor<V> extends Cursor<List<ScoredValue<V>>> {
}
