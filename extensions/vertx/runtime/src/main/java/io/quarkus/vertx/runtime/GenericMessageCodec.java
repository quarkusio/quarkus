package io.quarkus.vertx.runtime;

import java.io.*;
import java.lang.reflect.Method;

import org.jboss.logging.Logger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GenericMessageCodec implements MessageCodec<Object, Object> {
    private static final Logger LOGGER = Logger.getLogger(GenericMessageCodec.class.getName());

    @Override
    public void encodeToWire(Buffer buffer, Object o) {
        // Encode object to byte[]
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(b);
            oos.writeObject(o);
            oos.close();
            buffer.appendBytes(b.toByteArray());
        } catch (IOException e) {
            LOGGER.error("cannot write object to buffer", e);
        }
    }

    @Override
    public Object decodeFromWire(int position, Buffer buffer) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(buffer.getBytes());
        try {
            ObjectInputStream inputStream = new ObjectInputStream(bais);
            return inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("cannot read object from buffer", e);
        }
        return null;
    }

    @Override
    public Object transform(Object object) {
        if (object instanceof Cloneable) {
            Method cloneMethod;
            try {
                cloneMethod = object.getClass().getMethod("clone");
                return cloneMethod.invoke(object);
            } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
                //ignore it and return the object itself
            }
        }
        return object;
    }

    @Override
    public String name() {
        // Each codec must have a unique name.
        // This is used to identify a codec when sending a message and for unregistering codecs.
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        // Always -1
        return -1;
    }
}
