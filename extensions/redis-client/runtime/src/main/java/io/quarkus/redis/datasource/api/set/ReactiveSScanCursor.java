package io.quarkus.redis.datasource.api.set;

import java.util.List;

import io.quarkus.redis.datasource.api.ReactiveCursor;

public interface ReactiveSScanCursor<V> extends ReactiveCursor<List<V>> {

}
