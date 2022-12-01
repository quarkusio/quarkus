package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.hash.HashScanCursor;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;

public class HashScanBlockingCursorImpl<K, V> implements HashScanCursor<K, V> {

    private final ReactiveHashScanCursor<K, V> reactive;
    private final Duration timeout;

    public HashScanBlockingCursorImpl(ReactiveHashScanCursor<K, V> reactive, Duration timeout) {
        this.timeout = timeout;
        this.reactive = reactive;
    }

    @Override
    public boolean hasNext() {
        return reactive.hasNext();
    }

    @Override
    public Map<K, V> next() {
        return reactive.next().await().atMost(timeout);
    }

    @Override
    public Iterable<Map.Entry<K, V>> toIterable() {
        return reactive.toMulti().subscribe().asIterable();
    }

    @Override
    public long cursorId() {
        return reactive.cursorId();
    }
}
