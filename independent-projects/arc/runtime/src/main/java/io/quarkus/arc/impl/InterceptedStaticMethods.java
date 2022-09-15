package io.quarkus.arc.impl;

import jakarta.interceptor.InvocationContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class InterceptedStaticMethods {

    private static final ConcurrentMap<String, InterceptedStaticMethod> METHODS = new ConcurrentHashMap<>();

    private InterceptedStaticMethods() {
    }

    public static void register(String key, InterceptedStaticMethod method) {
        METHODS.putIfAbsent(key, method);
    }

    public static Object aroundInvoke(String key, Object[] args) throws Exception {
        InterceptedStaticMethod method = METHODS.get(key);
        if (method == null) {
            throw new IllegalArgumentException("Intercepted method metadata not found for key: " + key);
        }
        return InvocationContexts.performAroundInvoke(null, method.metadata.method, method.forward, args, method.metadata.chain,
                method.metadata.bindings);
    }

    public static final class InterceptedStaticMethod {

        final Function<InvocationContext, Object> forward;
        final InterceptedMethodMetadata metadata;

        public InterceptedStaticMethod(Function<InvocationContext, Object> forward, InterceptedMethodMetadata metadata) {
            this.forward = forward;
            this.metadata = metadata;
        }

    }

    static void clear() {
        METHODS.clear();
    }

}
