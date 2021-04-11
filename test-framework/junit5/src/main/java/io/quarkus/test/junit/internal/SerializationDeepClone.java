package io.quarkus.test.junit.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * Cloning strategy that just serializes and deserializes using plain old java serialization.
 */
class SerializationDeepClone implements DeepClone {

    private final ClassLoader classLoader;

    SerializationDeepClone(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Object clone(Object objectToClone) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(512);
        try (ObjectOutputStream objOut = new ObjectOutputStream(byteOut)) {
            objOut.writeObject(objectToClone);
            try (ObjectInputStream objIn = new ClassLoaderAwareObjectInputStream(byteOut)) {
                return objIn.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to deep clone object of type '" + objectToClone.getClass().getName()
                    + "'. Please report the issue on the Quarkus issue tracker.", e);
        }
    }

    private class ClassLoaderAwareObjectInputStream extends ObjectInputStream {

        public ClassLoaderAwareObjectInputStream(ByteArrayOutputStream byteOut) throws IOException {
            super(new ByteArrayInputStream(byteOut.toByteArray()));
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
            return Class.forName(desc.getName(), true, classLoader);
        }
    }
}
