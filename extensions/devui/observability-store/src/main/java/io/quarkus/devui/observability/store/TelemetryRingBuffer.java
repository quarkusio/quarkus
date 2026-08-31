package io.quarkus.devui.observability.store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A capped FIFO buffer. Oldest elements are evicted once the {@link BoundingStrategy}
 * reports the buffer has grown past its bound. All operations are synchronized so it
 * can be written from span-completion threads and read from the Dev UI thread.
 */
public final class TelemetryRingBuffer<T> {

    private final Deque<T> deque = new ArrayDeque<>();
    private final BoundingStrategy bounding;

    public TelemetryRingBuffer(BoundingStrategy bounding) {
        this.bounding = bounding;
    }

    public synchronized void add(T item) {
        deque.addLast(item);
        while (!deque.isEmpty() && bounding.exceedsBound(deque.size())) {
            deque.removeFirst();
        }
    }

    public synchronized List<T> snapshot() {
        return new ArrayList<>(deque);
    }

    public synchronized void clear() {
        deque.clear();
    }

    public synchronized int size() {
        return deque.size();
    }
}
