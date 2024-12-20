package io.quarkus.vertx.core.runtime;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

import io.vertx.core.Context;
import io.vertx.core.spi.context.storage.AccessMode;

final class QuarkusAccessModes {

    /**
     * Beware this {@link AccessMode#getOrCreate(AtomicReferenceArray, int, Supplier)} because, differently from
     * {@link io.vertx.core.spi.context.storage.ContextLocal#get(Context, Supplier)},
     * is not suitable to be used with {@link io.vertx.core.spi.context.storage.ContextLocal#get(Context, AccessMode, Supplier)}
     * with the same guarantees of atomicity i.e. the supplier can get called more than once by different racing threads!
     */
    public static final AccessMode ACQUIRE_RELEASE = new AccessMode() {
        @Override
        public Object get(AtomicReferenceArray<Object> locals, int idx) {
            return locals.getAcquire(idx);
        }

        @Override
        public void put(AtomicReferenceArray<Object> locals, int idx, Object value) {
            // This is still ensuring visibility across threads and happens-before,
            // but won't impose setVolatile total ordering i.e. StoreLoad barriers after write
            // to make it faster
            locals.setRelease(idx, value);
        }

        @Override
        public Object getOrCreate(AtomicReferenceArray<Object> locals, int idx, Supplier<Object> initialValueSupplier) {
            Object value = locals.getAcquire(idx);
            if (value == null) {
                value = initialValueSupplier.get();
                locals.setRelease(idx, value);
            }
            return value;
        }
    };

}
