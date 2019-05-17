package io.quarkus.runtime.serial;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A deserialization utility.
 */
public final class Deserializer {
    private Deserializer() {
    }

    private static final Map<String, Class<?>> primitives;

    static {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("boolean", boolean.class);

        map.put("byte", byte.class);
        map.put("short", short.class);
        map.put("int", int.class);
        map.put("long", long.class);

        map.put("float", float.class);
        map.put("double", double.class);

        map.put("char", char.class);

        map.put("void", void.class);
        primitives = map;
    }

    /**
     * Deserialize the given resource, throwing an error if it cannot be deserialized.
     *
     * @param type the result type class (must not be {@code null})
     * @param name the resource to deserialize (must not be {@code null})
     * @param <T> the result type
     * @return the deserialized result
     */
    public static <T> T deserializeResource(Class<T> type, String name) {
        final ClassLoader classLoader = Deserializer.class.getClassLoader();
        return deserializeResource(type, name, classLoader);
    }

    static <T> T deserializeResource(final Class<T> type, final String name, final ClassLoader classLoader) {
        final URL url = classLoader == null ? ClassLoader.getSystemResource(name) : classLoader.getResource(name);
        if (url == null) {
            throw new Error("Cannot deserialize due to missing " + name);
        }
        try (InputStream stream = url.openStream()) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                try (ObjectInputStream ois = new ObjectInputStream(bis) {
                    protected Class<?> resolveClass(final ObjectStreamClass desc) throws ClassNotFoundException {
                        final String className = desc.getName();
                        final Class<?> clazz = primitives.get(className);
                        if (clazz != null) {
                            return clazz;
                        }
                        return Class.forName(className, false, classLoader);
                    }
                }) {
                    return type.cast(ois.readObject());
                }
            }
        } catch (Exception e) {
            throw new Error("Failed to deserialize " + name, e);
        }
    }
}
