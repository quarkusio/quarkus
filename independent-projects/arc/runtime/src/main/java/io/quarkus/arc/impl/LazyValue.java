package io.quarkus.arc.impl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 *
 * @author Martin Kouba
 */
public class LazyValue<T> {

    private final Supplier<T> supplier;

    private final Lock lock = new ReentrantLock();

    private transient volatile T value;

    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        T valueCopy = value;
        if (valueCopy != null) {
            return valueCopy;
        }

        lock.lock();
        try {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    public T getIfPresent() {
        return value;
    }

    public void clear() {
        lock.lock();
        value = null;
        lock.unlock();
    }

    public boolean isSet() {
        return value != null;
    }

}
