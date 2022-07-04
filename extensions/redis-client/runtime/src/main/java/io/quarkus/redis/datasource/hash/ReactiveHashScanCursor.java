package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.ReactiveCursor;

public interface ReactiveHashScanCursor<K, V> extends ReactiveCursor<Map<K, V>> {
}
