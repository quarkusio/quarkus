package io.quarkus.redis.datasource.impl;

import java.time.Duration;
import java.util.Set;

import io.quarkus.redis.datasource.api.Cursor;
import io.quarkus.redis.datasource.api.ReactiveCursor;

public class ScanBlockingCursorImpl<K, V> implements Cursor<Set<K>> {

    private final ReactiveCursor<Set<K>> reactive;
    private final Duration timeout;

    public ScanBlockingCursorImpl(ReactiveCursor<Set<K>> reactive, Duration timeout) {
        this.timeout = timeout;
        this.reactive = reactive;
    }

    @Override
    public boolean hasNext() {
        return reactive.hasNext();
    }

    @Override
    public Set<K> next() {
        return reactive.next().await().atMost(timeout);
    }

    @Override
    public long cursorId() {
        return reactive.cursorId();
    }
}
