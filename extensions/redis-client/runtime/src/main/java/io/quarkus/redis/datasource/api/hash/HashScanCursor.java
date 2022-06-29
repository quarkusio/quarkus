package io.quarkus.redis.datasource.api.hash;

import java.util.Map;

import io.quarkus.redis.datasource.api.Cursor;

public interface HashScanCursor<K, V> extends Cursor<Map<K, V>> {
}
