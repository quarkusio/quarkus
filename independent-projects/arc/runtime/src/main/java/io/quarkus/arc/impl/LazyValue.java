package io.quarkus.arc.impl;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 *
 * @author Martin Kouba
 */
public class LazyValue<T> {

    private static final AtomicReferenceFieldUpdater<LazyValue, Object> VALUE_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(LazyValue.class, Object.class, "value");

    private final Supplier<T> supplier;

    private transient volatile T value;

    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        T valueCopy = value;
        if (valueCopy != null) {
            return valueCopy;
        }
        synchronized (this) {
            valueCopy = value;
            if (valueCopy != null) {
                return valueCopy;
            }
            valueCopy = supplier.get();
            // lazySet == setRelease: it ensure safe publication of the instance
            VALUE_UPDATER.lazySet(this, valueCopy);
            return valueCopy;
        }
    }

    public T getIfPresent() {
        return value;
    }

    public void clear() {
        if (value == null) {
            return;
        }
        synchronized (this) {
            if (value != null) {
                VALUE_UPDATER.lazySet(this, null);
            }
        }
    }

    public boolean isSet() {
        return value != null;
    }

}
