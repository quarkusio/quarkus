package io.quarkus.redis.datasource.set;

import java.util.List;

import io.quarkus.redis.datasource.ReactiveCursor;

public interface ReactiveSScanCursor<V> extends ReactiveCursor<List<V>> {

}
