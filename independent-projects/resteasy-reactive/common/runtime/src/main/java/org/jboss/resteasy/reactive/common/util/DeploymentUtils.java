package org.jboss.resteasy.reactive.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;

public abstract class DeploymentUtils {
    private static final Map<String, Class<?>> primitiveTypes;

    static {
        Map<String, Class<?>> prims = new HashMap<>();
        prims.put(byte.class.getName(), byte.class);
        prims.put(boolean.class.getName(), boolean.class);
        prims.put(char.class.getName(), char.class);
        prims.put(short.class.getName(), short.class);
        prims.put(int.class.getName(), int.class);
        prims.put(float.class.getName(), float.class);
        prims.put(double.class.getName(), double.class);
        prims.put(long.class.getName(), long.class);
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public static void registerWriter(Serialisers serialisers, String entityClassName,
            ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public static void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader reader) {
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
