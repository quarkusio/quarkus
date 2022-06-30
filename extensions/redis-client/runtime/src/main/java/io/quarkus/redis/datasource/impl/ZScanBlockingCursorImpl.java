package io.quarkus.redis.datasource.impl;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.api.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.api.sortedset.ScoredValue;
import io.quarkus.redis.datasource.api.sortedset.ZScanCursor;

public class ZScanBlockingCursorImpl<V> implements ZScanCursor<V> {

    private final ReactiveZScanCursor<V> reactive;
    private final Duration timeout;

    public ZScanBlockingCursorImpl(ReactiveZScanCursor<V> reactive, Duration timeout) {
        this.timeout = timeout;
        this.reactive = reactive;
    }

    @Override
    public boolean hasNext() {
        return reactive.hasNext();
    }

    @Override
    public List<ScoredValue<V>> next() {
        return reactive.next().await().atMost(timeout);
    }

    @Override
    public long cursorId() {
        return reactive.cursorId();
    }
}
