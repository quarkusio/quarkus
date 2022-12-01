package io.quarkus.vertx;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * An implementation of {@link MessageCodec} for local delivery only.
 * It does not support the clustered event bus.
 * <p>
 * The {@link #transform(Object)} method returns the passed instance. So make sure it's immutable.
 *
 * @param <T> the type of object supported by this codec.
 */
public class LocalEventBusCodec<T> implements MessageCodec<T, T> {

    // We need a counter to generate unique name as the event bus does not support having 2 codecs with the same name
    // even if they are targeting different types.
    private static final AtomicInteger count = new AtomicInteger();
    private final String name;

    public LocalEventBusCodec() {
        this(LocalEventBusCodec.class.getName() + "-" + count.getAndIncrement());
    }

    public LocalEventBusCodec(String name) {
        this.name = name;
    }

    @Override
    public void encodeToWire(Buffer buffer, T t) {
        throw new UnsupportedOperationException("LocalEventBusCodec cannot only be used for local delivery");
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        throw new UnsupportedOperationException("LocalEventBusCodec cannot only be used for local delivery");
    }

    @Override
    public T transform(T instance) {
        return instance;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
