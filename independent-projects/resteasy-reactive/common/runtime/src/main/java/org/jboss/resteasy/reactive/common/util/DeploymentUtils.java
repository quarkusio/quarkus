package org.jboss.resteasy.reactive.common.util;

import java.util.Map;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;

public abstract class DeploymentUtils {
    private static final Map<String, Class<?>> primitiveTypes = Map.of(byte.class.getName(), byte.class,
            boolean.class.getName(), boolean.class, char.class.getName(), char.class, short.class.getName(),
            short.class, int.class.getName(), int.class, float.class.getName(), float.class, double.class.getName(),
            double.class, long.class.getName(), long.class);

    public static void registerWriter(Serialisers serialisers, String entityClassName, ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public static void registerReader(Serialisers serialisers, String entityClassName, ResourceReader reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }

    public static <T> Class<T> loadClass(String name) {
        if (primitiveTypes.containsKey(name)) {
            return (Class<T>) primitiveTypes.get(name);
        }
        try {
            return (Class<T>) Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
