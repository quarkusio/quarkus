package io.quarkus.redis.datasource.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveCursor;

public interface ReactiveZScanCursor<V> extends ReactiveCursor<List<ScoredValue<V>>> {

}
