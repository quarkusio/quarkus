package org.jboss.resteasy.reactive.common.core;

import jakarta.ws.rs.client.InvocationCallback;
import java.util.HashMap;
import java.util.Map;

public class GenericTypeMapping {

    private Map<Class<? extends InvocationCallback<?>>, Class<?>> invocationCallbacks = new HashMap<>();

    public <T> void addInvocationCallback(Class<? extends InvocationCallback<T>> clazz, Class<T> resolvedType) {
        invocationCallbacks.put(clazz, resolvedType);
    }

    public Class<?> forInvocationCallback(Class<?> clazz) {
        return invocationCallbacks.get(clazz);
    }
}
