package io.quarkus.core.deployment.action.impl;

import java.io.IOException;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.smallrye.serial.Serialized;
import io.smallrye.serial.spi.ObjectSerializer;

/**
 * A serializer which handles {@code QuarkusClassLoader} instances.
 */
public final class QuarkusClassLoaderSerializer implements ObjectSerializer {
    static final ObjectSerializer INSTANCE = new QuarkusClassLoaderSerializer();

    private QuarkusClassLoaderSerializer() {
    }

    public Serialized serialize(final Context ctxt, final Object object) throws IOException {
        if (object instanceof QuarkusClassLoader) {
            // todo: improve this detection
            return SerializedQuarkusClassLoader.of(SerializedQuarkusClassLoader.Kind.RUNTIME);
        }
        return ctxt.next();
    }

    public int priority() {
        return PRIORITY_CLASS_LOADER + 1;
    }
}
