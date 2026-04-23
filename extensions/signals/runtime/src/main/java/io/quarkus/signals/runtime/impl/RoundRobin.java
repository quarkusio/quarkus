package io.quarkus.signals.runtime.impl;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Selects elements in round-robin order from an immutable sequence.
 * <p>
 * Thread-safe: an {@link AtomicInteger} drives the position, so concurrent
 * {@link #next()} calls rotate through the elements without locking.
 *
 * @param <T> the element type
 */
class RoundRobin<T> implements Iterable<T> {

    private final AtomicInteger pos = new AtomicInteger();
    private final List<T> elements;

    RoundRobin(List<T> elements) {
        this.elements = List.copyOf(elements);
    }

    /**
     * Returns the next element in round-robin order, cycling back to the first element
     * after the last one has been returned.
     *
     * @return the next element, or {@code null} when the sequence is empty
     */
    T next() {
        if (elements.isEmpty()) {
            return null;
        } else if (elements.size() == 1) {
            return elements.get(0);
        } else {
            return elements.get(Integer.remainderUnsigned(pos.getAndIncrement(), elements.size()));
        }
    }

    int size() {
        return elements.size();
    }

    boolean isEmpty() {
        return elements.isEmpty();
    }

    List<T> elements() {
        return elements;
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }
}
