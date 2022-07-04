package io.quarkus.redis.datasource.hash;

import java.util.Map;

import io.quarkus.redis.datasource.Cursor;

public interface HashScanCursor<K, V> extends Cursor<Map<K, V>> {
}
