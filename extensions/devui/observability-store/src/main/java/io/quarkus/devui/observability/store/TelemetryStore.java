package io.quarkus.devui.observability.store;

import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;

/**
 * Backend-agnostic dev-mode telemetry store: a capped ring buffer (the source of
 * truth) plus a best-effort live broadcast for the Dev UI. Generic over the record
 * type so each signal (traces, metrics, ...) instantiates its own store.
 *
 * The broadcast is non-blocking: {@link #record} pushes to the buffer first, then
 * emits to the broadcaster. {@link #stream()} drops on overflow, so a slow or
 * absent Dev UI socket can never apply backpressure to the calling (often request)
 * thread.
 */
// FIXME traces store
public final class TelemetryStore<T> {

    private final TelemetryRingBuffer<T> buffer;
    private final BroadcastProcessor<T> broadcaster = BroadcastProcessor.create();
    // record() is called concurrently from many span-completion threads, but
    // BroadcastProcessor.onNext() is not internally serialized. Emitting through the
    // serialized wrapper serializes producers so we honour the Reactive Streams
    // contract; it delegates to the same broadcaster stream() exposes.
    private final SerializedProcessor<T, T> emitter = broadcaster.serialized();

    public TelemetryStore(BoundingStrategy bounding) {
        this.buffer = new TelemetryRingBuffer<>(bounding);
    }

    /**
     * Records an item. {@code item} must be non-null (nulls are rejected by the
     * underlying buffer/broadcaster with a {@link NullPointerException}).
     */
    public void record(T item) {
        buffer.add(item); // source of truth, always succeeds
        emitter.onNext(item); // best-effort, serialized across producers; no subscriber -> dropped
    }

    public List<T> snapshot() {
        return buffer.snapshot();
    }

    /**
     * Live stream of newly recorded items. Drops on overflow so consumers can never
     * slow down the producer. Under concurrent recording the live stream order may
     * differ slightly from {@link #snapshot()} order (they are not strictly
     * cross-consistent), which is acceptable for a best-effort dev feed.
     */
    public Multi<T> stream() {
        return broadcaster.onOverflow().drop();
    }

    public void clear() {
        buffer.clear();
    }

    public int size() {
        return buffer.size();
    }
}
