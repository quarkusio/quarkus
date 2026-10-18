package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.datasource.set.SScanCursor;

public class SScanBlockingCursorImpl<V> implements SScanCursor<V> {

    private final ReactiveSScanCursor<V> reactive;
    private final Duration timeout;

    public SScanBlockingCursorImpl(ReactiveSScanCursor<V> reactive, Duration timeout) {
        this.timeout = timeout;
        this.reactive = reactive;
    }

    @Override
    public boolean hasNext() {
        return reactive.hasNext();
    }

    @Override
    public List<V> next() {
        return reactive.next().await().atMost(timeout);
    }

    @Override
    public long cursorId() {
        return reactive.cursorId();
    }

    @Override
    public Iterable<V> toIterable() {
        return reactive.toMulti().subscribe().asIterable();
    }
}
