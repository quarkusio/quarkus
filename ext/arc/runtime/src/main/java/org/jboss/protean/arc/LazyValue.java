package org.jboss.protean.arc;

import java.util.function.Supplier;

/**
 *
 * @author Martin Kouba
 */
public class LazyValue<T> {

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
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }

    public T getIfPresent() {
        return value;
    }

    public void clear() {
        synchronized (this) {
            value = null;
        }
    }

    public boolean isSet() {
        return value != null;
    }

}
