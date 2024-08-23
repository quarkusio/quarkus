package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.sortedset.ReactiveZScanCursor;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.ZScanCursor;

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

    @Override
    public Iterable<ScoredValue<V>> toIterable() {
        return reactive.toMulti().subscribe().asIterable();
    }
}
