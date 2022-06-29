package io.quarkus.redis.datasource.api.hash;

import java.util.Map;

import io.quarkus.redis.datasource.api.ReactiveCursor;

public interface ReactiveHashScanCursor<K, V> extends ReactiveCursor<Map<K, V>> {
}
