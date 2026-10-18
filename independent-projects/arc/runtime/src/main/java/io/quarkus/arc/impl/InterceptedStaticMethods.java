package io.quarkus.arc.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InterceptedStaticMethods {

    private static final ConcurrentMap<String, InterceptedMethodMetadata> METADATA = new ConcurrentHashMap<>();

    private InterceptedStaticMethods() {
    }

    public static void register(String key, InterceptedMethodMetadata metadata) {
        METADATA.putIfAbsent(key, metadata);
    }

    public static Object aroundInvoke(String key, Object[] args) throws Exception {
        InterceptedMethodMetadata metadata = METADATA.get(key);
        if (metadata == null) {
            throw new IllegalArgumentException("Intercepted method metadata not found for key: " + key);
        }
        return InvocationContexts.performAroundInvoke(null, args, metadata);
    }

    static void clear() {
        METADATA.clear();
    }

}
