package io.quarkus.redis.datasource.api.sortedset;

import java.util.List;

import io.quarkus.redis.datasource.api.ReactiveCursor;

public interface ReactiveZScanCursor<V> extends ReactiveCursor<List<ScoredValue<V>>> {

}
