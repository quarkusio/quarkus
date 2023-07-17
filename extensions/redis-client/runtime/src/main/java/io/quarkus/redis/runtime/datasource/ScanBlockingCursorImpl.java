package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Set;

import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;

public class ScanBlockingCursorImpl<K> implements KeyScanCursor<K> {

    private final ReactiveKeyScanCursor<K> reactive;
    private final Duration timeout;

    public ScanBlockingCursorImpl(ReactiveKeyScanCursor<K> reactive, Duration timeout) {
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

    @Override
    public Iterable<K> toIterable() {
        return reactive.toMulti().subscribe().asIterable();
    }
}
